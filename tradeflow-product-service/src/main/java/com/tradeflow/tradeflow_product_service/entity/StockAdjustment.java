package com.tradeflow.tradeflow_product_service.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_adjustments", uniqueConstraints = {@UniqueConstraint(name = "uk_stock_adjustment_request_id", columnNames = "request_id")},
        indexes = {@Index(name = "idx_stock_adjustment_product_id", columnList = "product_id")})
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, updatable = false, length = 36)
    private String requestId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private StockAdjustmentType type;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(nullable = false, length = 500, updatable = false)
    private String reason;

    @Column(name = "resulting_stock", nullable = false, updatable = false)
    private Integer resultingStock;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public StockAdjustment() {
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId.toString();
    }


    public Integer getResultingStock() {
        return resultingStock;
    }

    public void setResultingStock(Integer resultingStock) {
        this.resultingStock = resultingStock;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public StockAdjustmentType getType() {
        return type;
    }

    public void setType(StockAdjustmentType type) {
        this.type = type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}