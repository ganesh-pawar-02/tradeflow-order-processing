package com.tradeflow.tradeflow_notification_service.service;

import com.tradeflow.tradeflow_notification_service.entity.Notification;
import com.tradeflow.tradeflow_notification_service.event.OrderConfirmedEvent;
import com.tradeflow.tradeflow_notification_service.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    public NotificationPersistenceService(
            NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertNotification(OrderConfirmedEvent event) {

        Notification notification = new Notification();

        notification.setEventId(event.eventId());
        notification.setEventType(event.eventType());
        notification.setOrderId(event.orderId());
        notification.setCustomerId(event.customerId());
        notification.setTotalAmount(event.totalAmount());
        notification.setOccurredAt(event.occurredAt());

        notificationRepository.saveAndFlush(notification);
    }
}