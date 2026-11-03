package com.tradeflow.tradeflow_product_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ProductCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheInvalidator.class);

    private final CacheManager cacheManager;

    public ProductCacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAfterCommit(Long productId) {
        Runnable eviction = () -> {
            try {
                Cache products = cacheManager.getCache("products");
                Cache productList = cacheManager.getCache("productList");

                if (products != null) {
                    products.evict(productId);
                }

                if (productList != null) {
                    productList.clear();
                }
            } catch (RuntimeException ex) {
                // The database transaction has already committed.
                // Log this for monitoring; do not make a committed
                // stock operation appear to have failed to its caller.
                log.error(
                        "Product cache invalidation failed for productId={}",
                        productId,
                        ex
                );
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