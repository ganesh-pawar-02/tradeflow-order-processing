package com.tradeflow.tradeflow_product_service.dto;

import com.tradeflow.tradeflow_product_service.entity.ReservationStatus;

import java.util.UUID;

public record InventoryReservationResponse(
        UUID reservationId,
        Long orderId,
        Long productId,
        Integer quantity,
        ReservationStatus status
) {}

