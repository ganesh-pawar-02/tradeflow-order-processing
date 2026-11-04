package com.tradeflow.tradeflow_order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.tradeflow_order_service.entity.OutboxEvent;
import com.tradeflow.tradeflow_order_service.event.OrderConfirmedEvent;
import com.tradeflow.tradeflow_order_service.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveOrderConfirmedEvent(OrderConfirmedEvent event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    event.eventId().toString(),
                    "ORDER",
                    event.orderId().toString(),
                    "OrderConfirmed",
                    payload
            );

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize OrderConfirmed event",
                    exception
            );
        }
    }
}