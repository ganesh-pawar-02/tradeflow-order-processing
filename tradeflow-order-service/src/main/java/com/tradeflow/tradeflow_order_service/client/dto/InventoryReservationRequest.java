package com.tradeflow.tradeflow_order_service.client.dto;

import java.util.UUID;

public record InventoryReservationRequest(
        UUID reservationId,
        Long orderId,
        Long productId,
        Integer quantity
) {
}