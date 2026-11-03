package com.tradeflow.tradeflow_product_service.repository;

import com.tradeflow.tradeflow_product_service.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockAdjustmentRepository
        extends JpaRepository<StockAdjustment, Long> {

    Optional<StockAdjustment> findByRequestId(String requestId);
}