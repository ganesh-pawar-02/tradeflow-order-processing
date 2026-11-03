
package com.tradeflow.tradeflow_product_service.service;

import com.tradeflow.tradeflow_product_service.dto.ProductDto;
import com.tradeflow.tradeflow_product_service.dto.ProductResponse;
import com.tradeflow.tradeflow_product_service.dto.ProductUpdateRequest;
import com.tradeflow.tradeflow_product_service.entity.Product;
import com.tradeflow.tradeflow_product_service.exception.InsufficientStockException;
import com.tradeflow.tradeflow_product_service.exception.InvalidRequestException;
import com.tradeflow.tradeflow_product_service.exception.ResourceNotFoundException;
import com.tradeflow.tradeflow_product_service.repository.ProductRepository;

import org.modelmapper.ModelMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final CacheManager cacheManager;
    private final ProductCacheInvalidator cacheInvalidator;

    public ProductService(
            ProductRepository productRepository,
            ModelMapper modelMapper,
            CacheManager cacheManager, ProductCacheInvalidator cacheInvalidator) {

        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
        this.cacheManager = cacheManager;
        this.cacheInvalidator=cacheInvalidator;
    }

    // CREATE PRODUCT
    @Transactional
    public ProductResponse createProduct(ProductDto productDto) {

        validateStockQuantity(productDto.getStockQuantity());

        Product product = modelMapper.map(productDto, Product.class);

        Product savedProduct = productRepository.save(product);

        cacheInvalidator.evictAfterCommit(savedProduct.getId());

        return modelMapper.map(savedProduct, ProductResponse.class);
    }

    // GET PRODUCT BY ID
    @Transactional(readOnly = true)
    public ProductResponse findProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + id
                        )
                );

        return modelMapper.map(product, ProductResponse.class);
    }

    // GET ALL PRODUCTS
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {

        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(product ->
                        modelMapper.map(product, ProductResponse.class)
                )
                .toList();
    }

    // UPDATE PRODUCT
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + id
                        )
                );

        product.setName(request.name());
        product.setCategory(request.category());
        product.setPrice(request.price());

        Product savedProduct = productRepository.save(product);

        cacheInvalidator.evictAfterCommit(id);

        return modelMapper.map(savedProduct, ProductResponse.class);
    }

    // UPDATE STOCK QUANTITY
    @Transactional
    public ProductResponse updateProduct(Long id, Integer stock) {

        validateStockQuantity(stock);

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + id
                        )
                );

        product.setStockQuantity(stock);

        Product savedProduct = productRepository.save(product);

        cacheInvalidator.evictAfterCommit(id);

        return modelMapper.map(savedProduct, ProductResponse.class);
    }

    // RESERVE STOCK
    @Transactional
    public void reserveStock(Long productId, Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new InvalidRequestException(
                    "Quantity must be greater than zero"
            );
        }

        int updatedRows = productRepository.reserveStock(
                productId,
                quantity
        );

        if (updatedRows == 0) {

            if (!productRepository.existsById(productId)) {
                throw new ResourceNotFoundException(
                        "Product not found: " + productId
                );
            }

            throw new InsufficientStockException(
                    "Insufficient stock for product: " + productId
            );
        }

        cacheInvalidator.evictAfterCommit(productId);;
    }

    // VALIDATE STOCK
    private void validateStockQuantity(Integer stock) {

        if (stock == null || stock < 0) {
            throw new InvalidRequestException(
                    "Stock quantity cannot be null or negative"
            );
        }
    }

    // EVICT CACHES ONLY AFTER TRANSACTION COMMIT
    private void evictProductCachesAfterCommit(Long productId) {

        Runnable eviction = () -> {
            Cache productsCache = cacheManager.getCache("products");
            Cache productListCache = cacheManager.getCache("productList");

            if (productsCache != null) {
                productsCache.evict(productId);
            }

            if (productListCache != null) {
                productListCache.clear();
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            eviction.run();
                        }
                    }
            );

        } else {
            eviction.run();
        }
    }
}
