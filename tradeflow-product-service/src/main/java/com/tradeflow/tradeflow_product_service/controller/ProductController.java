package com.tradeflow.tradeflow_product_service.controller;

import com.tradeflow.tradeflow_product_service.dto.*;
import com.tradeflow.tradeflow_product_service.service.InventoryReservationService;
import com.tradeflow.tradeflow_product_service.service.ProductService;
import com.tradeflow.tradeflow_product_service.service.StockAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    public final ProductService productService;
    public final InventoryReservationService inventoryReservationService;
    private final StockAdjustmentService stockAdjustmentService;

    public ProductController(ProductService productService, InventoryReservationService inventoryReservationService, StockAdjustmentService stockAdjustmentService) {
        this.productService = productService;
        this.inventoryReservationService = inventoryReservationService;
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @PostMapping()
    public ProductResponse createProduct(@RequestBody ProductDto productDto) {
        return productService.createProduct(productDto);
    }

    @GetMapping("/{id}")
    public ProductResponse findProduct(@PathVariable Long id) {
        return productService.findProduct(id);
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAll();
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest productDto) {
        return productService.updateProduct(id, productDto);
    }


    @PatchMapping("/{id}/reserve-stock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reserveStock(@PathVariable Long id, @RequestParam Integer quantity) {

        productService.reserveStock(id, quantity);
    }

    @PostMapping("/{id}/reservations")
    public ResponseEntity<InventoryReservationResponse> reserveInventory(@PathVariable Long id, @Valid @RequestBody ReserveInventoryRequest request) {
        InventoryReservationResponse response = inventoryReservationService.reserve(id, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reservations/{reservationId}/release")
    public ResponseEntity<InventoryReservationResponse> releaseInventory(@PathVariable Long id, @PathVariable UUID reservationId) {
        InventoryReservationResponse response =
                inventoryReservationService.release(id, reservationId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reservations/{reservationId}/commit")
    public ResponseEntity<InventoryReservationResponse> commitInventory(@PathVariable Long id, @PathVariable UUID reservationId) {

        InventoryReservationResponse response = inventoryReservationService.commit(id, reservationId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/stock-adjustments")
    public ResponseEntity<StockAdjustmentResponse> adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {

        return ResponseEntity.ok(
                stockAdjustmentService.adjustStock(id, request)
        );
    }
}
