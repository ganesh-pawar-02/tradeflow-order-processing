
package com.tradeflow.tradeflow_order_service.entity;

public enum OrderStatus {
    PENDING,
    COMMITTING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    COMPENSATION_REQUIRED
}
