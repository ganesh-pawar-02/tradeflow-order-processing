package com.tradeflow.tradeflow_order_service.client.dto;

import java.util.UUID;

public record InventoryReservationResponse(
        UUID reservationId,
        Long orderId,
        Long productId,
        Integer quantity,
        InventoryReservationStatus status
) {
}