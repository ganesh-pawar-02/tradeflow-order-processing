package com.tradeflow.tradeflow_product_service.service;

import com.tradeflow.tradeflow_product_service.dto.InventoryReservationResponse;
import com.tradeflow.tradeflow_product_service.dto.ReserveInventoryRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventoryReservationService {

    private final InventoryReservationTransactionService transactionService;

    public InventoryReservationService(InventoryReservationTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public InventoryReservationResponse reserve(Long productId, ReserveInventoryRequest request) {
        try {
            return transactionService.reserveNew(productId, request);
        } catch (DataIntegrityViolationException exception) {
            // A duplicate reservation ID may mean this is a retry.
            // Check the existing record in a fresh transaction.
            return transactionService.getExistingReservation(
                    productId,
                    request
            );
        }
    }

    public InventoryReservationResponse release(Long productId, UUID reservationId) {
        return transactionService.release(productId, reservationId);
    }

    public InventoryReservationResponse commit(Long productId, UUID reservationId) {

        return transactionService.commit(productId, reservationId);
    }
}