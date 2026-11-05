
package com.tradeflow.tradeflow_notification_service.controller;

import com.tradeflow.tradeflow_notification_service.dto.MarkAllReadResponse;
import com.tradeflow.tradeflow_notification_service.dto.NotificationResponse;
import com.tradeflow.tradeflow_notification_service.service.NotificationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // GET /notifications?customerId=1&page=0&size=20
    @GetMapping
    public Page<NotificationResponse> getNotifications(
            @RequestParam Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return notificationService.getNotifications(
                customerId,
                pageable
        );
    }

    // GET /notifications/10?customerId=1
    @GetMapping("/{notificationId}")
    public NotificationResponse getNotification(
            @PathVariable Long notificationId,
            @RequestParam Long customerId) {

        return notificationService.getNotification(
                notificationId,
                customerId
        );
    }

    // GET /notifications/unread-count?customerId=1
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(
            @RequestParam Long customerId) {

        long count = notificationService.getUnreadCount(customerId);

        return new UnreadCountResponse(count);
    }

    // PATCH /notifications/10/read?customerId=1
    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long customerId) {

        return notificationService.markAsRead(
                notificationId,
                customerId
        );
    }

    // PATCH /notifications/read-all?customerId=1
    @PatchMapping("/read-all")
    public MarkAllReadResponse markAllAsRead(
            @RequestParam Long customerId) {

        return notificationService.markAllAsRead(customerId);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
