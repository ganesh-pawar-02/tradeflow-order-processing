package com.tradeflow.tradeflow_order_service.service;

import com.tradeflow.tradeflow_order_service.entity.Order;
import com.tradeflow.tradeflow_order_service.entity.OrderItem;
import com.tradeflow.tradeflow_order_service.entity.OrderItemReservationStatus;
import com.tradeflow.tradeflow_order_service.entity.OrderStatus;
import com.tradeflow.tradeflow_order_service.event.OrderConfirmedEvent;
import com.tradeflow.tradeflow_order_service.exception.ResourceNotFoundException;
import com.tradeflow.tradeflow_order_service.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Service
public class OrderSagaPersistenceService {

    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;

    public OrderSagaPersistenceService(OrderRepository orderRepository, OutboxEventService outboxEventService) {
        this.orderRepository = orderRepository;
        this.outboxEventService = outboxEventService;
    }

    // --------------------------------------------------
    // NORMAL ORDER SAGA OPERATIONS
    // --------------------------------------------------

    @Transactional
    public Long createPendingOrder(Order order) {

        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.saveAndFlush(order);

        return savedOrder.getId();
    }

    @Transactional
    public void updateItemReservationStatus(
            Long orderId,
            String reservationId,
            OrderItemReservationStatus status) {

        Order order = orderRepository.findOrderWithItems(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        OrderItem item = findOrderItem(order, reservationId);

        item.setReservationStatus(status);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        boolean newlyConfirmed =
                status == OrderStatus.CONFIRMED
                        && order.getStatus() != OrderStatus.CONFIRMED;

        order.setStatus(status);

        if (newlyConfirmed) {
            createOrderConfirmedOutboxEvent(order);
        }
    }

    /**
     * Schedules a retry during the initial Saga execution.
     * <p>
     * This method is used before a recovery worker has claimed
     * the order, so it does not require a recovery token.
     */
    @Transactional
    public void scheduleInitialRecoveryRetry(
            Long orderId,
            Instant nextRetryAt) {

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.COMMITTING) {

            throw new IllegalStateException(
                    "Only PENDING or COMMITTING orders can schedule "
                            + "an initial recovery retry"
            );
        }

        order.setNextRetryAt(nextRetryAt);
    }

    @Transactional(readOnly = true)
    public Order findOrder(Long orderId) {

        return orderRepository.findOrderWithItems(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );
    }

    // --------------------------------------------------
    // RECOVERY CLAIM AND LEASE OPERATIONS
    // --------------------------------------------------

    @Transactional
    public boolean claimRecoveryOrder(
            Long orderId,
            Collection<OrderStatus> statuses,
            Instant now,
            Instant leaseUntil,
            String token) {

        return orderRepository.claimRecoveryOrder(
                orderId,
                statuses,
                now,
                leaseUntil,
                token
        ) == 1;
    }

    @Transactional(readOnly = true)
    public Order findClaimedOrder(Long orderId, String token) {

        Order order = orderRepository.findOrderWithItems(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        verifyRecoveryLease(order, token, Instant.now());

        return order;
    }

    @Transactional
    public boolean renewRecoveryLease(
            Long orderId,
            String token,
            Instant now,
            Instant newLeaseUntil) {

        return orderRepository.renewRecoveryLease(
                orderId,
                token,
                now,
                newLeaseUntil
        ) == 1;
    }

    // --------------------------------------------------
    // RECOVERY ITEM UPDATES
    // --------------------------------------------------

    @Transactional
    public void updateItemReservationStatusForRecovery(
            Long orderId,
            String reservationId,
            OrderItemReservationStatus status,
            String token) {

        Order order = findOrderForRecoveryUpdate(orderId);

        verifyRecoveryLease(order, token, Instant.now());

        OrderItem item = findOrderItem(order, reservationId);

        item.setReservationStatus(status);
    }

    // --------------------------------------------------
    // RECOVERY ORDER STATUS TRANSITIONS
    // --------------------------------------------------

    @Transactional
    public void transitionRecoveryStatus(
            Long orderId,
            String token,
            OrderStatus newStatus) {

        Order order = findOrderForRecoveryUpdate(orderId);

        verifyRecoveryLease(order, token, Instant.now());

        order.setStatus(newStatus);
    }

    @Transactional
    public void finishRecovery(
            Long orderId,
            String token,
            OrderStatus finalStatus) {

        Order order = findOrderForRecoveryUpdate(orderId);

        verifyRecoveryLease(order, token, Instant.now());

        boolean newlyConfirmed =
                finalStatus == OrderStatus.CONFIRMED
                        && order.getStatus() != OrderStatus.CONFIRMED;

        order.setStatus(finalStatus);
        order.setNextRetryAt(null);
        clearRecoveryLease(order);

        if (newlyConfirmed) {
            createOrderConfirmedOutboxEvent(order);
        }
    }

    @Transactional
    public void finishRecoveryWithRetry(
            Long orderId,
            String token,
            OrderStatus status,
            Instant nextRetryAt) {

        Order order = findOrderForRecoveryUpdate(orderId);

        verifyRecoveryLease(order, token, Instant.now());

        order.setStatus(status);
        order.setNextRetryAt(nextRetryAt);

        clearRecoveryLease(order);
    }

    @Transactional
    public void scheduleRecoveryRetry(
            Long orderId,
            String token,
            Instant nextRetryAt) {

        Order order = findOrderForRecoveryUpdate(orderId);

        verifyRecoveryLease(order, token, Instant.now());

        // Keep the current Saga status.
        order.setNextRetryAt(nextRetryAt);

        clearRecoveryLease(order);
    }

    // --------------------------------------------------
    // PRIVATE HELPERS
    // --------------------------------------------------

    /**
     * Always acquire the database row lock before modifying
     * recovery-owned order state.
     */
    private Order findOrderForRecoveryUpdate(Long orderId) {

        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );
    }

    private OrderItem findOrderItem(
            Order order,
            String reservationId) {

        return order.getItems()
                .stream()
                .filter(item ->
                        reservationId.equals(item.getReservationId())
                )
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order item reservation not found: "
                                        + reservationId
                        )
                );
    }

    private void clearRecoveryLease(Order order) {

        order.setRecoveryToken(null);
        order.setRecoveryLeaseUntil(null);
    }

    private void verifyRecoveryLease(
            Order order,
            String token,
            Instant now) {

        if (token == null
                || order.getRecoveryToken() == null
                || !order.getRecoveryToken().equals(token)) {

            throw new IllegalStateException(
                    "Recovery lease is no longer owned by this worker"
            );
        }

        if (order.getRecoveryLeaseUntil() == null
                || !order.getRecoveryLeaseUntil().isAfter(now)) {

            throw new IllegalStateException(
                    "Recovery lease has expired"
            );
        }
    }

    private void createOrderConfirmedOutboxEvent(Order order) {

        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(),
                "OrderConfirmed",
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                Instant.now()
        );

        outboxEventService.saveOrderConfirmedEvent(event);
    }
}