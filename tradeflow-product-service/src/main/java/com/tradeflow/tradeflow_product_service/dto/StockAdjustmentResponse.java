package com.tradeflow.tradeflow_product_service.dto;

import com.tradeflow.tradeflow_product_service.entity.StockAdjustmentType;

import java.time.Instant;
import java.util.UUID;

public record StockAdjustmentResponse(
        UUID requestId,
        Long productId,
        StockAdjustmentType type,
        Integer quantity,
        String reason,
        Integer resultingStock,
        Instant createdAt
) {
}