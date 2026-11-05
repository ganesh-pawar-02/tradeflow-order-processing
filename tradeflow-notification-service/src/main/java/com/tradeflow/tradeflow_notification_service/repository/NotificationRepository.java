
package com.tradeflow.tradeflow_notification_service.repository;

import com.tradeflow.tradeflow_notification_service.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    boolean existsByEventId(UUID eventId);

    Page<Notification> findAllByCustomerId(
            Long customerId,
            Pageable pageable
    );

    Optional<Notification> findByIdAndCustomerId(
            Long id,
            Long customerId
    );

    long countByCustomerIdAndReadFalse(Long customerId);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        update Notification n
        set n.read = true,
            n.readAt = :readAt
        where n.id = :notificationId
          and n.customerId = :customerId
          and n.read = false
        """)
    int markAsRead(
            @Param("notificationId") Long notificationId,
            @Param("customerId") Long customerId,
            @Param("readAt") Instant readAt
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        update Notification n
        set n.read = true,
            n.readAt = :readAt
        where n.customerId = :customerId
          and n.read = false
        """)
    int markAllAsRead(
            @Param("customerId") Long customerId,
            @Param("readAt") Instant readAt
    );
}
