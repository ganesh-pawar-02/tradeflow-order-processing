package com.tradeflow.tradeflow_order_service;

import com.tradeflow.tradeflow_order_service.client.ProductClient;
import com.tradeflow.tradeflow_order_service.client.dto.ProductResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    public final ProductClient productClient;
    public OrderController(ProductClient productClient){
        this.productClient=productClient;
    }
    @GetMapping("/product/{id}")
    public ProductResponse getProduct(@PathVariable Long id){
        return productClient.getProduct(id);
    }
}
