
package com.tradeflow.tradeflow_notification_service.service;

import com.tradeflow.tradeflow_notification_service.dto.MarkAllReadResponse;
import com.tradeflow.tradeflow_notification_service.dto.NotificationResponse;
import com.tradeflow.tradeflow_notification_service.entity.Notification;
import com.tradeflow.tradeflow_notification_service.event.OrderConfirmedEvent;
import com.tradeflow.tradeflow_notification_service.repository.NotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPersistenceService persistenceService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPersistenceService persistenceService) {
        this.notificationRepository = notificationRepository;
        this.persistenceService = persistenceService;
    }

    // Existing Kafka persistence logic
    @Transactional(readOnly = true)
    public void saveOrderConfirmedNotification(OrderConfirmedEvent event) {

        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info(
                    "Duplicate event ignored. eventId={}",
                    event.eventId()
            );
            return;
        }

        try {
            persistenceService.insertNotification(event);

            log.info(
                    "Notification saved. eventId={}, orderId={}",
                    event.eventId(),
                    event.orderId()
            );

        } catch (DataIntegrityViolationException exception) {

            if (notificationRepository.existsByEventId(event.eventId())) {
                log.info(
                        "Concurrent duplicate event ignored. eventId={}",
                        event.eventId()
                );
                return;
            }

            throw exception;
        }
    }

    // Get paginated notifications for one customer
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(
            Long customerId,
            Pageable pageable) {

        validateCustomerId(customerId);

        return notificationRepository
                .findAllByCustomerId(customerId, pageable)
                .map(NotificationResponse::from);
    }

    // Get one notification belonging to the specified customer
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(
            Long notificationId,
            Long customerId) {

        validateCustomerId(customerId);
        validateNotificationId(notificationId);

        Notification notification = notificationRepository
                .findByIdAndCustomerId(notificationId, customerId)
                .orElseThrow(() -> notificationNotFound());

        return NotificationResponse.from(notification);
    }

    // Get unread notification count
    @Transactional(readOnly = true)
    public long getUnreadCount(Long customerId) {

        validateCustomerId(customerId);

        return notificationRepository.countByCustomerIdAndReadFalse(
                customerId
        );
    }

    // Mark one notification as read
    @Transactional
    public NotificationResponse markAsRead(
            Long notificationId,
            Long customerId) {

        validateCustomerId(customerId);
        validateNotificationId(notificationId);

        notificationRepository.markAsRead(
                notificationId,
                customerId,
                Instant.now()
        );

        // Also handles already-read notifications.
        // The original readAt timestamp remains unchanged.
        Notification notification = notificationRepository
                .findByIdAndCustomerId(notificationId, customerId)
                .orElseThrow(() -> notificationNotFound());

        return NotificationResponse.from(notification);
    }

    // Mark all unread notifications for one customer as read
    @Transactional
    public MarkAllReadResponse markAllAsRead(Long customerId) {

        validateCustomerId(customerId);

        int updatedCount = notificationRepository.markAllAsRead(
                customerId,
                Instant.now()
        );

        return new MarkAllReadResponse(updatedCount);
    }

    private void validateCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer ID must be a positive number"
            );
        }
    }

    private void validateNotificationId(Long notificationId) {
        if (notificationId == null || notificationId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notification ID must be a positive number"
            );
        }
    }

    private ResponseStatusException notificationNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Notification not found"
        );
    }
}
