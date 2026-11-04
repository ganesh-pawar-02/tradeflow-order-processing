package com.tradeflow.tradeflow_notification_service.service;

import com.tradeflow.tradeflow_notification_service.entity.Notification;
import com.tradeflow.tradeflow_notification_service.event.OrderConfirmedEvent;
import com.tradeflow.tradeflow_notification_service.repository.NotificationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void saveOrderConfirmedNotification(OrderConfirmedEvent event) {

        Notification notification = new Notification();

        notification.setEventId(event.eventId());
        notification.setEventType(event.eventType());
        notification.setOrderId(event.orderId());
        notification.setCustomerId(event.customerId());
        notification.setTotalAmount(event.totalAmount());
        notification.setOccurredAt(event.occurredAt());

        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException exception) {
            // A duplicate event ID may already have been persisted.
            throw exception;
        }
    }
}