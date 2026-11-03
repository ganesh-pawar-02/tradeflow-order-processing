package com.tradeflow.tradeflow_order_service.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.tradeflow.tradeflow_order_service.client.dto.ProductResponse;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradeflow.tradeflow_order_service.client.ProductClient;
import com.tradeflow.tradeflow_order_service.dto.*;
import com.tradeflow.tradeflow_order_service.entity.*;
import com.tradeflow.tradeflow_order_service.repository.OrderRepository;


@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = new Order();

        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.items()) {

            ProductResponse product = productClient.getProduct(itemRequest.productId());

            // Validate product status
            if (!"ACTIVE".equalsIgnoreCase(product.status())) {
                throw new IllegalArgumentException("Product is not active: " + product.id());
            }

            // Validate stock
            if (product.stockQuantity() < itemRequest.quantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.id());
            }

            // Calculate subtotal
            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));

            totalAmount = totalAmount.add(subtotal);

            // Create OrderItem
            OrderItem orderItem = new OrderItem();

            orderItem.setProductId(product.id());
            orderItem.setProductName(product.name());
            orderItem.setUnitPrice(product.price());
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setSubtotal(subtotal);

            order.addItem(orderItem);

            itemResponses.add(
                    new OrderItemResponse(
                            product.id(),
                            product.name(),
                            product.price(),
                            itemRequest.quantity(),
                            subtotal
                    )
            );
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus(),
                itemResponses,
                savedOrder.getCreatedAt(),
                savedOrder.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Long customerId, Pageable pageable) {

        Page<Order> orders = customerId != null
                ? orderRepository.findByCustomerId(customerId, pageable)
                : orderRepository.findAll(pageable);

        return orders.map(order -> {

            List<OrderItemResponse> items = order.getItems()
                    .stream()
                    .map(item -> new OrderItemResponse(
                            item.getProductId(),
                            item.getProductName(),
                            item.getUnitPrice(),
                            item.getQuantity(),
                            item.getSubtotal()
                    ))
                    .toList();

            return new OrderResponse(
                    order.getId(),
                    order.getCustomerId(),
                    order.getTotalAmount(),
                    order.getStatus(),
                    items,
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        });
    }
}