package com.tradeflow.tradeflow_notification_service.event;

import com.tradeflow.tradeflow_notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "tradeflow.order.events",
            groupId = "tradeflow-notification-group"
    )
    public void consumeOrderConfirmed(OrderConfirmedEvent event) {

        notificationService.saveOrderConfirmedNotification(event);

        log.info(
                "Order notification persisted successfully. eventId={}, orderId={}",
                event.eventId(),
                event.orderId()
        );
    }
}