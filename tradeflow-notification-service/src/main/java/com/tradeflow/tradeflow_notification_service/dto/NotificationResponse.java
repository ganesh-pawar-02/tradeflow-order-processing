
package com.tradeflow.tradeflow_notification_service.dto;

import com.tradeflow.tradeflow_notification_service.entity.Notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        Long id,
        UUID eventId,
        String eventType,
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        Instant occurredAt,
        Instant createdAt,
        boolean read,
        Instant readAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getEventType(),
                notification.getOrderId(),
                notification.getCustomerId(),
                notification.getTotalAmount(),
                notification.getOccurredAt(),
                notification.getCreatedAt(),
                notification.isRead(),
                notification.getReadAt()
        );
    }
}
