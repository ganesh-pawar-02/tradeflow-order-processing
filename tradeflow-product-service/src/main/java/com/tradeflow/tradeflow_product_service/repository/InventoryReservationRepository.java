package com.tradeflow.tradeflow_product_service.repository;

import java.util.Optional;
import java.util.UUID;

import com.tradeflow.tradeflow_product_service.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tradeflow.tradeflow_product_service.entity.InventoryReservation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByReservationId(String reservationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE InventoryReservation r
    SET r.status = :newStatus
    WHERE r.reservationId = :reservationId
      AND r.status = :currentStatus
    """)
    int updateStatus(@Param("reservationId") String reservationId, @Param("currentStatus") ReservationStatus currentStatus, @Param("newStatus") ReservationStatus newStatus);
}