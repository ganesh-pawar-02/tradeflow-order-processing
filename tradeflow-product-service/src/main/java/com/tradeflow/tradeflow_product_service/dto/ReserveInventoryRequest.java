package com.tradeflow.tradeflow_product_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReserveInventoryRequest(
        @NotNull UUID reservationId,
        @NotNull @Positive Long orderId,
        @NotNull @Positive Integer quantity
) {}