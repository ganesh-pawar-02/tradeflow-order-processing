
package com.tradeflow.tradeflow_order_service.service;

import com.tradeflow.tradeflow_order_service.client.ProductClient;
import com.tradeflow.tradeflow_order_service.client.dto.InventoryReservationRequest;
import com.tradeflow.tradeflow_order_service.client.dto.InventoryReservationResponse;
import com.tradeflow.tradeflow_order_service.client.dto.InventoryReservationStatus;

import com.tradeflow.tradeflow_order_service.entity.Order;
import com.tradeflow.tradeflow_order_service.entity.OrderItem;
import com.tradeflow.tradeflow_order_service.entity.OrderItemReservationStatus;
import com.tradeflow.tradeflow_order_service.entity.OrderStatus;
import com.tradeflow.tradeflow_order_service.repository.OrderRepository;

import feign.FeignException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderSagaRecoveryService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderSagaRecoveryService.class);

    private static final List<OrderStatus> RECOVERABLE_STATUSES =
            List.of(
                    OrderStatus.PENDING,
                    OrderStatus.COMMITTING,
                    OrderStatus.COMPENSATION_REQUIRED
            );

    private static final int BATCH_SIZE = 25;

    private static final Duration LEASE_DURATION =
            Duration.ofMinutes(2);

    private static final Duration RETRY_DELAY =
            Duration.ofSeconds(60);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderSagaPersistenceService persistenceService;

    public OrderSagaRecoveryService(
            OrderRepository orderRepository,
            ProductClient productClient,
            OrderSagaPersistenceService persistenceService) {

        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.persistenceService = persistenceService;
    }

    @Scheduled(fixedDelayString =
            "${tradeflow.saga.recovery-delay-ms:30000}")
    public void recoverOrders() {

        Instant now = Instant.now();

        List<Long> candidates =
                orderRepository.findRecoveryCandidates(
                        RECOVERABLE_STATUSES,
                        now,
                        PageRequest.of(0, BATCH_SIZE)
                ).getContent();

        for (Long orderId : candidates) {
            recoverOneOrder(orderId);
        }
    }

    private void recoverOneOrder(Long orderId) {

        Instant now = Instant.now();
        String token = UUID.randomUUID().toString();

        boolean claimed = persistenceService.claimRecoveryOrder(
                orderId,
                RECOVERABLE_STATUSES,
                now,
                now.plus(LEASE_DURATION),
                token
        );

        if (!claimed) {
            return;
        }

        try {
            Order order =
                    persistenceService.findClaimedOrder(orderId, token);

            switch (order.getStatus()) {

                case PENDING ->
                        recoverPendingOrder(order, token);

                case COMMITTING ->
                        recoverCommittingOrder(order, token);

                case COMPENSATION_REQUIRED ->
                        recoverCompensation(order, token);

                default ->
                        log.warn(
                                "Unexpected recovery status for order {}: {}",
                                orderId,
                                order.getStatus()
                        );
            }

        } catch (RuntimeException ex) {

            log.error(
                    "Saga recovery failed for order {}",
                    orderId,
                    ex
            );

            // If this worker still owns the lease, schedule a retry.
            // If its lease expired, this call safely fails.
            try {
                persistenceService.scheduleRecoveryRetry(
                        orderId,
                        token,
                        Instant.now().plus(RETRY_DELAY)
                );
            } catch (RuntimeException leaseException) {
                log.warn(
                        "Could not schedule recovery retry for order {}. "
                                + "The lease may have expired.",
                        orderId,
                        leaseException
                );
            }
        }
    }

    private void recoverPendingOrder(Order order, String token) {

        for (OrderItem item : order.getItems()) {

            if (item.getReservationStatus()
                    == OrderItemReservationStatus.RELEASED
                    || item.getReservationStatus()
                    == OrderItemReservationStatus.COMMITTED) {

                log.error(
                        "Unexpected reservation state {} for PENDING order {}, item {}",
                        item.getReservationStatus(),
                        order.getId(),
                        item.getId()
                );

                scheduleRetry(order.getId(), token);
                return;
            }

            try {
                renewLease(order.getId(), token);

                InventoryReservationRequest request =
                        new InventoryReservationRequest(
                                UUID.fromString(item.getReservationId()),
                                order.getId(),
                                item.getProductId(),
                                item.getQuantity()
                        );

                InventoryReservationResponse response =
                        productClient.reserveInventory(
                                item.getProductId(),
                                request
                        );

                if (response == null
                        || response.status()
                        != InventoryReservationStatus.RESERVED) {

                    log.warn(
                            "Reservation not confirmed for order {}, product {}",
                            order.getId(),
                            item.getProductId()
                    );

                    scheduleRetry(order.getId(), token);
                    return;
                }

                persistenceService.updateItemReservationStatusForRecovery(
                        order.getId(),
                        item.getReservationId(),
                        OrderItemReservationStatus.RESERVED,
                        token
                );

            } catch (FeignException ex) {

                if (isDefinitiveReservationRejection(ex)) {

                    log.warn(
                            "Reservation rejected for order {}, product {}, HTTP {}",
                            order.getId(),
                            item.getProductId(),
                            ex.status()
                    );

                    compensateAfterReservationFailure(order, token);
                    return;
                }

                // A timeout or server error does not prove that the
                // reservation failed. Retry with the same UUID.
                log.warn(
                        "Temporary reservation failure for order {}, product {}",
                        order.getId(),
                        item.getProductId(),
                        ex
                );

                scheduleRetry(order.getId(), token);
                return;

            } catch (RuntimeException ex) {

                log.warn(
                        "Reservation processing failed for order {}, product {}",
                        order.getId(),
                        item.getProductId(),
                        ex
                );

                scheduleRetry(order.getId(), token);
                return;
            }
        }

        // Every item is reserved. Move to COMMITTING under the lease.
        persistenceService.transitionRecoveryStatus(
                order.getId(),
                token,
                OrderStatus.COMMITTING
        );

        recoverCommittingOrder(order, token);
    }

    private boolean isDefinitiveReservationRejection(
            FeignException exception) {

        int status = exception.status();

        return status == 400
                || status == 404
                || status == 409
                || status == 422;
    }

    private void compensateAfterReservationFailure(
            Order order,
            String token) {

        boolean released = releaseAllReservations(order, token);

        if (released) {
            persistenceService.finishRecovery(
                    order.getId(),
                    token,
                    OrderStatus.CANCELLED
            );
        } else {
            persistenceService.finishRecoveryWithRetry(
                    order.getId(),
                    token,
                    OrderStatus.COMPENSATION_REQUIRED,
                    Instant.now().plus(RETRY_DELAY)
            );
        }
    }

    private void recoverCommittingOrder(Order order, String token) {

        for (OrderItem item : order.getItems()) {

            if (item.getReservationStatus()
                    == OrderItemReservationStatus.COMMITTED) {
                continue;
            }

            if (item.getReservationStatus()
                    == OrderItemReservationStatus.RELEASED) {

                log.error(
                        "Cannot commit released reservation {} for order {}",
                        item.getReservationId(),
                        order.getId()
                );

                scheduleRetry(order.getId(), token);
                return;
            }

            try {
                renewLease(order.getId(), token);

                InventoryReservationResponse response =
                        productClient.commitReservation(
                                item.getProductId(),
                                UUID.fromString(item.getReservationId())
                        );

                if (response == null
                        || response.status()
                        != InventoryReservationStatus.COMMITTED) {

                    scheduleRetry(order.getId(), token);
                    return;
                }

                persistenceService.updateItemReservationStatusForRecovery(
                        order.getId(),
                        item.getReservationId(),
                        OrderItemReservationStatus.COMMITTED,
                        token
                );

            } catch (RuntimeException ex) {

                // Do not compensate here. Earlier items may already
                // be committed, and commit is idempotent.
                log.warn(
                        "Commit retry failed for order {}, product {}",
                        order.getId(),
                        item.getProductId(),
                        ex
                );

                scheduleRetry(order.getId(), token);
                return;
            }
        }

        persistenceService.finishRecovery(
                order.getId(),
                token,
                OrderStatus.CONFIRMED
        );
    }

    private void recoverCompensation(Order order, String token) {

        boolean released = releaseAllReservations(order, token);

        if (released) {
            persistenceService.finishRecovery(
                    order.getId(),
                    token,
                    OrderStatus.CANCELLED
            );
        } else {
            persistenceService.scheduleRecoveryRetry(
                    order.getId(),
                    token,
                    Instant.now().plus(RETRY_DELAY)
            );
        }
    }

    private boolean releaseAllReservations(Order order, String token) {

        boolean allReleased = true;

        for (OrderItem item : order.getItems()) {

            if (item.getReservationStatus()
                    == OrderItemReservationStatus.RELEASED) {
                continue;
            }

            // A committed reservation cannot be released by the
            // normal compensation endpoint.
            if (item.getReservationStatus()
                    == OrderItemReservationStatus.COMMITTED) {

                log.error(
                        "Reservation {} for order {} is already committed; "
                                + "manual reconciliation may be required",
                        item.getReservationId(),
                        order.getId()
                );

                allReleased = false;
                continue;
            }

            try {
                renewLease(order.getId(), token);

                InventoryReservationResponse response =
                        productClient.releaseReservation(
                                item.getProductId(),
                                UUID.fromString(item.getReservationId())
                        );

                if (response == null
                        || response.status()
                        != InventoryReservationStatus.RELEASED) {

                    allReleased = false;
                    continue;
                }

                persistenceService.updateItemReservationStatusForRecovery(
                        order.getId(),
                        item.getReservationId(),
                        OrderItemReservationStatus.RELEASED,
                        token
                );

            } catch (FeignException.NotFound ex) {

                /*
                 * This assumes Product Service returns 404 here only
                 * when the reservation ID does not exist.
                 * Product deletion must not make this assumption unsafe.
                 */
                persistenceService.updateItemReservationStatusForRecovery(
                        order.getId(),
                        item.getReservationId(),
                        OrderItemReservationStatus.RELEASED,
                        token
                );

            } catch (RuntimeException ex) {

                log.warn(
                        "Could not release reservation {} for order {}",
                        item.getReservationId(),
                        order.getId(),
                        ex
                );

                allReleased = false;
            }
        }

        return allReleased;
    }

    private void renewLease(Long orderId, String token) {

        Instant now = Instant.now();

        boolean renewed = persistenceService.renewRecoveryLease(
                orderId,
                token,
                now,
                now.plus(LEASE_DURATION)
        );

        if (!renewed) {
            throw new IllegalStateException(
                    "Recovery lease lost for order " + orderId
            );
        }
    }

    private void scheduleRetry(Long orderId, String token) {

        persistenceService.scheduleRecoveryRetry(
                orderId,
                token,
                Instant.now().plus(RETRY_DELAY)
        );
    }
}
