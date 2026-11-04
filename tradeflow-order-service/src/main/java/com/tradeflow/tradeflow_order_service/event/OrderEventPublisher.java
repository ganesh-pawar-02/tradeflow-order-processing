package com.tradeflow.tradeflow_order_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrderEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventPublisher.class);

    private static final String ORDER_EVENTS_TOPIC =
            "tradeflow.order.events";

    private final KafkaTemplate<String, OrderConfirmedEvent> kafkaTemplate;

    public OrderEventPublisher(
            KafkaTemplate<String, OrderConfirmedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<Void> publishOrderConfirmed(
            OrderConfirmedEvent event) {

        return kafkaTemplate.send(
                ORDER_EVENTS_TOPIC,
                event.orderId().toString(),
                event
        ).thenAccept(result ->
                log.info(
                        "Published OrderConfirmed event. orderId={}, partition={}, offset={}",
                        event.orderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                )
        );
    }
}