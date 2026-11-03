package com.tradeflow.tradeflow_product_service.dto;

import com.tradeflow.tradeflow_product_service.entity.StockAdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull
        UUID requestId,

        @NotNull
        StockAdjustmentType type,

        @NotNull
        @Positive
        Integer quantity,

        @NotBlank
        @Size(max = 500)
        String reason
) {
}