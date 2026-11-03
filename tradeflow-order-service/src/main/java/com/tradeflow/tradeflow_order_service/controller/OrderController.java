package com.tradeflow.tradeflow_order_service.controller;

import com.tradeflow.tradeflow_order_service.client.ProductClient;
import com.tradeflow.tradeflow_order_service.client.dto.ProductResponse;
import com.tradeflow.tradeflow_order_service.dto.CreateOrderRequest;
import com.tradeflow.tradeflow_order_service.dto.OrderResponse;
import com.tradeflow.tradeflow_order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    public final ProductClient productClient;
    private final OrderService orderService;

    public OrderController(ProductClient productClient, OrderService orderService){
        this.productClient=productClient;
        this.orderService=orderService;
    }
    @GetMapping("/product/{id}")
    public ProductResponse getProduct(@PathVariable Long id){
        return productClient.getProduct(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {

        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(@PathVariable Long orderId) {

        return orderService.getOrderById(orderId);
    }

    @GetMapping
    public Page<OrderResponse> getOrders(@RequestParam(required = false) Long customerId, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return orderService.getOrders(customerId, pageable);
    }
}
