package com.tradeflow.tradeflow_product_service.service;

import com.tradeflow.tradeflow_product_service.dto.StockAdjustmentRequest;
import com.tradeflow.tradeflow_product_service.dto.StockAdjustmentResponse;
import com.tradeflow.tradeflow_product_service.entity.Product;
import com.tradeflow.tradeflow_product_service.entity.StockAdjustment;
import com.tradeflow.tradeflow_product_service.exception.InvalidRequestException;
import com.tradeflow.tradeflow_product_service.exception.InvalidReservationStateException;
import com.tradeflow.tradeflow_product_service.exception.ResourceNotFoundException;
import com.tradeflow.tradeflow_product_service.repository.ProductRepository;
import com.tradeflow.tradeflow_product_service.repository.StockAdjustmentRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockAdjustmentService {

    private final ProductRepository productRepository;
    private final StockAdjustmentRepository adjustmentRepository;
    private final ProductCacheInvalidator cacheInvalidator;

    public StockAdjustmentService(ProductRepository productRepository, StockAdjustmentRepository adjustmentRepository, ProductCacheInvalidator cacheInvalidator) {

        this.productRepository = productRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Transactional
    public StockAdjustmentResponse adjustStock(Long productId, StockAdjustmentRequest request) {

        String requestId = request.requestId().toString();

        // Lock the product row before checking or changing stock.
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + productId
                        )
                );

        // An identical retry returns the original result.
        var existing = adjustmentRepository.findByRequestId(requestId);

        if (existing.isPresent()) {
            StockAdjustment previous = existing.get();

            validateSameRequest(previous, productId, request);

            return toResponse(previous, product.getStockQuantity());
        }

        int currentStock = product.getStockQuantity();
        int newStock;

        switch (request.type()) {
            case INCREASE -> {
                try {
                    newStock = Math.addExact(
                            currentStock,
                            request.quantity()
                    );
                } catch (ArithmeticException ex) {
                    throw new InvalidRequestException(
                            "Stock quantity exceeds the supported limit"
                    );
                }
            }

            case DECREASE -> {
                if (currentStock < request.quantity()) {
                    throw new InvalidRequestException(
                            "Insufficient stock for this adjustment"
                    );
                }

                newStock = currentStock - request.quantity();
            }

            default -> throw new InvalidRequestException(
                    "Unsupported stock adjustment type"
            );
        }

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setRequestId(request.requestId());
        adjustment.setProductId(productId);
        adjustment.setType(request.type());
        adjustment.setQuantity(request.quantity());
        adjustment.setReason(request.reason());

        /*
         * The unique constraint is the final protection against
         * concurrent reuse of the same request ID.
         */
        try {
            adjustment.setResultingStock(newStock);
            adjustmentRepository.saveAndFlush(adjustment);
        } catch (DataIntegrityViolationException ex) {
            /*
             * Do not continue using this transaction after a database
             * constraint failure. The transaction will roll back.
             * The caller can safely retry using the same requestId.
             */
            throw ex;
        }

        product.setStockQuantity(newStock);
        adjustment.setResultingStock(newStock);
        productRepository.save(product);

        cacheInvalidator.evictAfterCommit(productId);

        return new StockAdjustmentResponse(
                request.requestId(),
                productId,
                request.type(),
                request.quantity(),
                request.reason(),
                newStock,
                adjustment.getCreatedAt()
        );
    }

    private void validateSameRequest(StockAdjustment previous, Long productId, StockAdjustmentRequest request) {

        boolean sameRequest =
                previous.getProductId().equals(productId)
                        && previous.getType() == request.type()
                        && previous.getQuantity().equals(request.quantity())
                        && previous.getReason().equals(request.reason());

        if (!sameRequest) {
            throw new InvalidReservationStateException(
                    "This requestId has already been used for a different adjustment"
            );
        }
    }

    private StockAdjustmentResponse toResponse(StockAdjustment adjustment, Integer currentStock) {

        return new StockAdjustmentResponse(
                java.util.UUID.fromString(adjustment.getRequestId()),
                adjustment.getProductId(),
                adjustment.getType(),
                adjustment.getQuantity(),
                adjustment.getReason(),
                currentStock,
                adjustment.getCreatedAt()
        );
    }
}