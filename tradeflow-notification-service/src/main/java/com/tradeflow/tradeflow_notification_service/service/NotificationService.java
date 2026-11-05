package com.tradeflow.tradeflow_notification_service.service;

import com.tradeflow.tradeflow_notification_service.event.OrderConfirmedEvent;
import com.tradeflow.tradeflow_notification_service.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public void saveOrderConfirmedNotification(OrderConfirmedEvent event) {

        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate event ignored. eventId={}", event.eventId());
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

            // Confirm that the failure was caused by this event already existing.
            if (notificationRepository.existsByEventId(event.eventId())) {
                log.info(
                        "Concurrent duplicate event ignored. eventId={}",
                        event.eventId()
                );
                return;
            }

            // Do not hide unrelated database constraint violations.
            throw exception;
        }
    }
}