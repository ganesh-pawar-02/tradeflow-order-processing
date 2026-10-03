package com.tradeflow.tradeflow_product_service.repository;

import com.tradeflow.tradeflow_product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {
    @Override
    Optional<Product> findById(Long aLong);

    @Override
    List<Product> findAll();
}
