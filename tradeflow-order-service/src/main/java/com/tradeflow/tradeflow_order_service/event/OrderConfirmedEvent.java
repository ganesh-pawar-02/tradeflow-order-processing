package com.tradeflow.tradeflow_order_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        String eventType,
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        Instant occurredAt
) {
}