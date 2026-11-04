
package com.tradeflow.tradeflow_order_service.client;

import com.tradeflow.tradeflow_order_service.client.dto.ProductResponse;
import com.tradeflow.tradeflow_order_service.client.dto.InventoryReservationRequest;
import com.tradeflow.tradeflow_order_service.client.dto.InventoryReservationResponse;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "tradeflow-product-service")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponse getProduct(
            @PathVariable("id") Long id
    );

    // Reserve inventory
    @PostMapping("/products/{id}/reservations")
    InventoryReservationResponse reserveInventory(
            @PathVariable("id") Long productId,
            @RequestBody InventoryReservationRequest request
    );

    // Commit inventory reservation
    @PostMapping(
            "/products/{id}/reservations/{reservationId}/commit"
    )
    InventoryReservationResponse commitReservation(
            @PathVariable("id") Long productId,
            @PathVariable("reservationId") UUID reservationId
    );

    // Release inventory reservation
    @PostMapping(
            "/products/{id}/reservations/{reservationId}/release"
    )
    InventoryReservationResponse releaseReservation(
            @PathVariable("id") Long productId,
            @PathVariable("reservationId") UUID reservationId
    );
}
