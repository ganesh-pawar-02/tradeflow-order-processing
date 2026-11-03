
package com.tradeflow.tradeflow_product_service.repository;

import com.tradeflow.tradeflow_product_service.entity.Product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Atomically reserve stock.
    // Prevents two concurrent requests from overselling inventory.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Product p
        SET p.stockQuantity = p.stockQuantity - :quantity,
            p.version = p.version + 1
        WHERE p.id = :productId
          AND p.stockQuantity >= :quantity
          AND :quantity > 0
    """)
    int reserveStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );

    // Atomically restore stock.
    // Version increment invalidates stale entity versions.
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.stockQuantity = p.stockQuantity + :quantity,
            p.version = p.version + 1
        WHERE p.id = :productId
          AND :quantity > 0
    """)
    int releaseStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT p
    FROM Product p
    WHERE p.id = :productId
    """)
    Optional<Product> findByIdForUpdate(
            @Param("productId") Long productId
    );
}
