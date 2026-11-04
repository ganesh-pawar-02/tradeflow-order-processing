package com.tradeflow.tradeflow_order_service.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tradeflow.tradeflow_order_service.client.ProductClient;
import com.tradeflow.tradeflow_order_service.client.dto.ProductResponse;
import com.tradeflow.tradeflow_order_service.client.dto.InventoryReservationRequest;
import com.tradeflow.tradeflow_order_service.client.dto.InventoryReservationResponse;
import com.tradeflow.tradeflow_order_service.client.dto.InventoryReservationStatus;

import com.tradeflow.tradeflow_order_service.dto.*;
import com.tradeflow.tradeflow_order_service.entity.*;
import com.tradeflow.tradeflow_order_service.event.OrderConfirmedEvent;
import com.tradeflow.tradeflow_order_service.event.OrderEventPublisher;
import com.tradeflow.tradeflow_order_service.repository.OrderRepository;

import feign.FeignException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Duration INITIAL_RETRY_DELAY =
            Duration.ofSeconds(60);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderSagaPersistenceService persistenceService;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderService(
            OrderRepository orderRepository,
            ProductClient productClient,
            OrderSagaPersistenceService persistenceService) {

        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.persistenceService = persistenceService;
    }

    /*
     * Saga orchestrator.
     *
     * Intentionally NOT @Transactional:
     * Feign calls must not run inside a long database transaction.
     */
    public OrderResponse createOrder(CreateOrderRequest request) {

        // --------------------------------------------------
        // PHASE 1: FETCH PRODUCT INFORMATION AND PREPARE ORDER
        // --------------------------------------------------

        Order order = new Order();

        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {

            ProductResponse product =
                    productClient.getProduct(itemRequest.productId());

            BigDecimal subtotal = product.price()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));

            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();

            item.setProductId(product.id());
            item.setProductName(product.name());
            item.setUnitPrice(product.price());
            item.setQuantity(itemRequest.quantity());
            item.setSubtotal(subtotal);

            // Generate once and persist before reservation calls.
            // Every retry must reuse this same reservation ID.
            item.setReservationId(UUID.randomUUID().toString());

            item.setReservationStatus(
                    OrderItemReservationStatus.NOT_RESERVED
            );

            order.addItem(item);
        }

        order.setTotalAmount(totalAmount);

        // --------------------------------------------------
        // PHASE 2: PERSIST PENDING ORDER
        // --------------------------------------------------

        Long orderId = persistenceService.createPendingOrder(order);

        // --------------------------------------------------
        // PHASE 3: RESERVE ALL INVENTORY
        // --------------------------------------------------

        for (OrderItem item : order.getItems()) {

            try {

                InventoryReservationRequest reservationRequest =
                        new InventoryReservationRequest(
                                UUID.fromString(item.getReservationId()),
                                orderId,
                                item.getProductId(),
                                item.getQuantity()
                        );

                InventoryReservationResponse response =
                        productClient.reserveInventory(
                                item.getProductId(),
                                reservationRequest
                        );

                if (response == null
                        || response.status()
                        != InventoryReservationStatus.RESERVED) {

                    // Unexpected response: do not assume that the
                    // remote reservation failed.
                    scheduleInitialRetry(orderId);

                    return toResponse(
                            persistenceService.findOrder(orderId)
                    );
                }

                persistenceService.updateItemReservationStatus(
                        orderId,
                        item.getReservationId(),
                        OrderItemReservationStatus.RESERVED
                );

            } catch (FeignException ex) {

                if (isDefinitiveReservationRejection(ex)) {

                    // A definitive business rejection means this
                    // order cannot proceed with its current request.
                    boolean compensationCompleted =
                            compensateReservations(
                                    orderId,
                                    order.getItems()
                            );

                    persistenceService.updateOrderStatus(
                            orderId,
                            compensationCompleted
                                    ? OrderStatus.CANCELLED
                                    : OrderStatus.COMPENSATION_REQUIRED
                    );

                    return toResponse(
                            persistenceService.findOrder(orderId)
                    );
                }

                /*
                 * A timeout, service-unavailable response, or server
                 * error does not prove that the remote reservation
                 * failed. The Product Service may have completed it.
                 *
                 * Keep PENDING and retry with the same reservation ID.
                 */
                scheduleInitialRetry(orderId);

                return toResponse(
                        persistenceService.findOrder(orderId)
                );

            } catch (RuntimeException ex) {

                /*
                 * This also covers unexpected local processing failures.
                 * Do not assume the remote reservation did not happen.
                 */
                scheduleInitialRetry(orderId);

                return toResponse(
                        persistenceService.findOrder(orderId)
                );
            }
        }

        // --------------------------------------------------
        // PHASE 4: ALL RESERVATIONS SUCCEEDED
        // --------------------------------------------------

        persistenceService.updateOrderStatus(
                orderId,
                OrderStatus.COMMITTING
        );

        /*
         * Do not compensate if a commit fails partway through.
         *
         * Some reservations may already be COMMITTED and cannot be
         * released through the normal release API.
         *
         * Keep COMMITTING and schedule recovery.
         */

        // --------------------------------------------------
        // PHASE 5: COMMIT ALL RESERVATIONS
        // --------------------------------------------------

        for (OrderItem item : order.getItems()) {

            try {

                InventoryReservationResponse response =
                        productClient.commitReservation(
                                item.getProductId(),
                                UUID.fromString(item.getReservationId())
                        );

                if (response == null
                        || response.status()
                        != InventoryReservationStatus.COMMITTED) {

                    scheduleInitialRetry(orderId);

                    return toResponse(
                            persistenceService.findOrder(orderId)
                    );
                }

                persistenceService.updateItemReservationStatus(
                        orderId,
                        item.getReservationId(),
                        OrderItemReservationStatus.COMMITTED
                );

            } catch (RuntimeException commitFailure) {

                /*
                 * The remote commit may have succeeded even if the
                 * response was lost or the local update failed.
                 *
                 * Keep COMMITTING. The recovery worker retries the
                 * same reservation ID, relying on idempotent commit.
                 */
                scheduleInitialRetry(orderId);

                return toResponse(
                        persistenceService.findOrder(orderId)
                );
            }
        }

        // --------------------------------------------------
        // PHASE 6: ORDER CONFIRMED
        // --------------------------------------------------

        persistenceService.updateOrderStatus(
                orderId,
                OrderStatus.CONFIRMED
        );
        Order confirmedOrder = persistenceService.findOrder(orderId);

        return toResponse(
                persistenceService.findOrder(orderId)
        );
    }

    // --------------------------------------------------
    // RETRY HANDLING
    // --------------------------------------------------

    private void scheduleInitialRetry(Long orderId) {

        persistenceService.scheduleInitialRecoveryRetry(
                orderId,
                Instant.now().plus(INITIAL_RETRY_DELAY)
        );
    }

    // --------------------------------------------------
    // RESERVATION REJECTION CLASSIFICATION
    // --------------------------------------------------

    private boolean isDefinitiveReservationRejection(
            FeignException exception) {

        int status = exception.status();

        return status == 400
                || status == 404
                || status == 409
                || status == 422;
    }

    // --------------------------------------------------
    // COMPENSATION
    // --------------------------------------------------

    /**
     * Compensation is idempotent:
     * release the same reservation IDs, even if a previous call timed out.
     */
    private boolean compensateReservations(
            Long orderId,
            List<OrderItem> items) {

        boolean allReleased = true;

        for (OrderItem item : items) {

            try {

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

                persistenceService.updateItemReservationStatus(
                        orderId,
                        item.getReservationId(),
                        OrderItemReservationStatus.RELEASED
                );

            } catch (FeignException.NotFound notFound) {

                /*
                 * This assumes Product Service returns 404 here only
                 * when the reservation ID does not exist.
                 *
                 * If 404 can also mean that the product is missing,
                 * do not treat it as successful compensation. The
                 * Product Service API should distinguish these cases.
                 */
                persistenceService.updateItemReservationStatus(
                        orderId,
                        item.getReservationId(),
                        OrderItemReservationStatus.RELEASED
                );

            } catch (RuntimeException releaseFailure) {

                allReleased = false;
            }
        }

        return allReleased;
    }

    // --------------------------------------------------
    // READ OPERATIONS
    // --------------------------------------------------

    public OrderResponse getOrderById(Long orderId) {

        return toResponse(
                persistenceService.findOrder(orderId)
        );
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(
            Long customerId,
            Pageable pageable) {

        Page<Order> orders = customerId != null
                ? orderRepository.findByCustomerId(customerId, pageable)
                : orderRepository.findAll(pageable);

        return orders.map(this::toResponse);
    }

    // --------------------------------------------------
    // ENTITY TO RESPONSE CONVERSION
    // --------------------------------------------------

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}