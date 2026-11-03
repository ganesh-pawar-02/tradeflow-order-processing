package com.tradeflow.tradeflow_order_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.tradeflow.tradeflow_order_service.entity.OrderStatus;

public record OrderResponse(
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}