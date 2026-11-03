package com.tradeflow.tradeflow_product_service.service;

import com.tradeflow.tradeflow_product_service.dto.InventoryReservationResponse;
import com.tradeflow.tradeflow_product_service.dto.ReserveInventoryRequest;
import com.tradeflow.tradeflow_product_service.entity.InventoryReservation;
import com.tradeflow.tradeflow_product_service.entity.ReservationStatus;
import com.tradeflow.tradeflow_product_service.exception.InsufficientStockException;
import com.tradeflow.tradeflow_product_service.exception.InvalidReservationStateException;
import com.tradeflow.tradeflow_product_service.exception.ResourceNotFoundException;
import com.tradeflow.tradeflow_product_service.repository.InventoryReservationRepository;
import com.tradeflow.tradeflow_product_service.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventoryReservationTransactionService {

    private final InventoryReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    public InventoryReservationTransactionService(InventoryReservationRepository reservationRepository, ProductRepository productRepository) {
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public InventoryReservationResponse reserveNew(Long productId, ReserveInventoryRequest request) {
        // 1. Insert the reservation first.
        InventoryReservation reservation = new InventoryReservation();

        reservation.setReservationId(request.reservationId().toString());
        reservation.setOrderId(request.orderId());
        reservation.setProductId(productId);
        reservation.setQuantity(request.quantity());
        reservation.setStatus(ReservationStatus.RESERVED);

        // Flush immediately so the unique constraint is checked here.
        reservationRepository.saveAndFlush(reservation);

        // 2. Atomically deduct stock.
        int updatedRows = productRepository.reserveStock(
                productId,
                request.quantity()
        );

        if (updatedRows == 0) {
            // Runtime exception causes the transaction to roll back,
            // including the reservation insert.
            throw new InsufficientStockException(
                    "Product not found or insufficient stock for product: " + productId
            );
        }

        return new InventoryReservationResponse(
                UUID.fromString(reservation.getReservationId()),
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getQuantity(),
                reservation.getStatus()
        );
    }

    @Transactional
    public InventoryReservationResponse getExistingReservation(
            Long productId,
            ReserveInventoryRequest request
    ) {
        InventoryReservation existing = reservationRepository
                .findByReservationId(request.reservationId().toString())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found " + request.reservationId()
                ));

        // A reservation ID must always represent the same operation.
        if (!existing.getOrderId().equals(request.orderId())
                || !existing.getProductId().equals(productId)
                || !existing.getQuantity().equals(request.quantity())) {

            throw new InvalidReservationStateException(
                    "Reservation ID was already used with different reservation details"
            );
        }

        return new InventoryReservationResponse(
                UUID.fromString(existing.getReservationId()),
                existing.getOrderId(),
                existing.getProductId(),
                existing.getQuantity(),
                existing.getStatus()
        );
    }

    @Transactional
    public InventoryReservationResponse release(Long productId, UUID reservationId) {
        // 1. Attempt the state transition.
        int updatedRows = reservationRepository.updateStatus(reservationId.toString(), ReservationStatus.RESERVED, ReservationStatus.RELEASED);

        // 2. If no row was updated, inspect the existing state.
        if (updatedRows == 0) {
            InventoryReservation existing = reservationRepository
                    .findByReservationId(reservationId.toString())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Reservation not found: " + reservationId
                    ));

            if (!existing.getProductId().equals(productId)) {
                throw new InvalidReservationStateException(
                        "Reservation does not belong to product: " + productId
                );
            }

            if (existing.getStatus() == ReservationStatus.RELEASED) {
                // Already released: idempotent success.
                return new InventoryReservationResponse(
                        UUID.fromString(existing.getReservationId()),
                        existing.getOrderId(),
                        existing.getProductId(),
                        existing.getQuantity(),
                        existing.getStatus()
                );
            }

            throw new InvalidReservationStateException(
                    "Only RESERVED inventory can be released. Current status: "
                            + existing.getStatus()
            );
        }

        // 3. Load the reservation and restore its quantity.
        InventoryReservation reservation = reservationRepository
                .findByReservationId(reservationId.toString())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation disappeared during release"
                ));

        if (!reservation.getProductId().equals(productId)) {
            throw new InvalidReservationStateException(
                    "Reservation does not belong to product: " + productId
            );
        }

        int restoredRows = productRepository.releaseStock(productId, reservation.getQuantity());

        if (restoredRows == 0) {
            // Throwing rolls back the status update too.
            throw new IllegalStateException(
                    "Could not restore stock for product: " + productId
            );
        }

        return new InventoryReservationResponse(
                UUID.fromString(reservation.getReservationId()),
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getQuantity(),
                ReservationStatus.RELEASED
        );
    }

    @Transactional
    public InventoryReservationResponse commit(Long productId, UUID reservationId) {

        InventoryReservation reservation = reservationRepository
                .findByReservationId(reservationId.toString())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found: " + reservationId
                        )
                );

        if (!reservation.getProductId().equals(productId)) {
            throw new InvalidReservationStateException(
                    "Reservation does not belong to product: " + productId
            );
        }

        // Already committed: idempotent success.
        if (reservation.getStatus() == ReservationStatus.COMMITTED) {
            return new InventoryReservationResponse(
                    UUID.fromString(reservation.getReservationId()),
                    reservation.getOrderId(),
                    reservation.getProductId(),
                    reservation.getQuantity(),
                    reservation.getStatus()
            );
        }

        // A released reservation cannot be committed.
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            throw new InvalidReservationStateException(
                    "Cannot commit a released reservation"
            );
        }

        int updatedRows = reservationRepository.updateStatus(
                reservationId.toString(),
                ReservationStatus.RESERVED,
                ReservationStatus.COMMITTED
        );

        if (updatedRows == 0) {
            throw new InvalidReservationStateException(
                    "Reservation state changed. Commit could not be completed."
            );
        }

        InventoryReservation updatedReservation = reservationRepository
                .findByReservationId(reservationId.toString())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found: " + reservationId
                        )
                );

        return new InventoryReservationResponse(
                UUID.fromString(updatedReservation.getReservationId()),
                updatedReservation.getOrderId(),
                updatedReservation.getProductId(),
                updatedReservation.getQuantity(),
                updatedReservation.getStatus()
        );
    }
}