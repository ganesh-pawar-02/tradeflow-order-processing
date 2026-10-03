package com.tradeflow.tradeflow_order_service.client;

import com.tradeflow.tradeflow_order_service.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "tradeflow-product-service")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponse getProduct(@PathVariable Long id);
}
