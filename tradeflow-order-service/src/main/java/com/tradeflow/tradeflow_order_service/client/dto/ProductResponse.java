package com.tradeflow.tradeflow_order_service.client.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, Integer stockQuantity) {}
