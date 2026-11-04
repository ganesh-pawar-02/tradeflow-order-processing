
package com.tradeflow.tradeflow_order_service.repository;

import com.tradeflow.tradeflow_order_service.entity.Order;
import com.tradeflow.tradeflow_order_service.entity.OrderStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    // --------------------------------------------------
    // RECOVERY CANDIDATE SEARCH
    // --------------------------------------------------

    @Query("""
        SELECT o.id
        FROM Order o
        WHERE o.status IN :statuses
          AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)
          AND (o.recoveryLeaseUntil IS NULL
               OR o.recoveryLeaseUntil <= :now)
        ORDER BY o.createdAt ASC
        """)
    Page<Long> findRecoveryCandidates(@Param("statuses") Collection<OrderStatus> statuses, @Param("now") Instant now, Pageable pageable);

    // --------------------------------------------------
    // ATOMIC RECOVERY CLAIM
    // --------------------------------------------------

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Order o
        SET o.recoveryToken = :token,
            o.recoveryLeaseUntil = :leaseUntil,
            o.version = o.version + 1
        WHERE o.id = :orderId
          AND o.status IN :statuses
          AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)
          AND (o.recoveryLeaseUntil IS NULL
               OR o.recoveryLeaseUntil <= :now)
        """)
    int claimRecoveryOrder(@Param("orderId") Long orderId, @Param("statuses") Collection<OrderStatus> statuses, @Param("now") Instant now, @Param("leaseUntil") Instant leaseUntil, @Param("token") String token);

    // --------------------------------------------------
    // LOAD ORDER WITH ITEMS
    // --------------------------------------------------

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items
        WHERE o.id = :orderId
        """)
    Optional<Order> findOrderWithItems(@Param("orderId") Long orderId);

    // --------------------------------------------------
    // PESSIMISTIC LOCK FOR ORDER RECOVERY UPDATES
    // --------------------------------------------------

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    // --------------------------------------------------
    // RECOVERY LEASE RENEWAL
    // --------------------------------------------------

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Order o
        SET o.recoveryLeaseUntil = :leaseUntil,
            o.version = o.version + 1
        WHERE o.id = :orderId
          AND o.recoveryToken = :token
          AND o.recoveryLeaseUntil > :now
        """)
    int renewRecoveryLease(@Param("orderId") Long orderId, @Param("token") String token, @Param("now") Instant now, @Param("leaseUntil") Instant leaseUntil);
}
