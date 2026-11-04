package com.tradeflow.tradeflow_order_service.service;

import com.tradeflow.tradeflow_order_service.entity.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private static final String ORDER_EVENTS_TOPIC =
            "tradeflow.order.events";

    private final OutboxClaimService claimService;
    private final OutboxDeliveryService deliveryService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxClaimService claimService,
            OutboxDeliveryService deliveryService,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.claimService = claimService;
        this.deliveryService = deliveryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events = claimService.claimPendingEvents();

        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event) {

        String eventId = event.getEventId();
        String token = event.getLockToken();

        try {
            boolean leaseRenewed =
                    deliveryService.renewLease(eventId, token);

            if (!leaseRenewed) {
                log.warn(
                        "Skipping outbox event because lease renewal failed. eventId={}",
                        eventId
                );
                return;
            }

            kafkaTemplate.send(
                    ORDER_EVENTS_TOPIC,
                    event.getAggregateId(),
                    event.getPayload()
            ).get(30, TimeUnit.SECONDS);

            boolean updated =
                    deliveryService.markAsPublished(eventId, token);

            if (!updated) {
                log.warn(
                        "Could not mark event as published; ownership may have changed. eventId={}",
                        eventId
                );
            }

        } catch (Exception exception) {

            log.error(
                    "Failed to publish outbox event. eventId={}",
                    eventId,
                    exception
            );

            try {
                int attempts = event.getAttempts() + 1;

                long delaySeconds = Math.min(
                        300,
                        5L * (1L << Math.min(attempts - 1, 6))
                );

                deliveryService.scheduleRetry(
                        eventId,
                        token,
                        Instant.now().plusSeconds(delaySeconds),
                        exception.getMessage()
                );

            } catch (Exception retryException) {
                log.error(
                        "Failed to schedule retry. eventId={}",
                        eventId,
                        retryException
                );
            }
        }
    }
}