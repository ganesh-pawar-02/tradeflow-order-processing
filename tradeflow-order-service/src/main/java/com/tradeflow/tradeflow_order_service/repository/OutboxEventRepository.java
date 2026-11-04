package com.tradeflow.tradeflow_order_service.repository;

import com.tradeflow.tradeflow_order_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, String> {

    @Query(value = """
        SELECT event_id
        FROM order_outbox_events
        WHERE status = 'PENDING'
          AND next_attempt_at <= :now
          AND (locked_until IS NULL OR locked_until <= :now)
        ORDER BY created_at, event_id
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> findClaimableEventIds(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query(value = """
        UPDATE order_outbox_events
        SET lock_token = :token,
            locked_until = :lockedUntil
        WHERE event_id IN (:eventIds)
          AND status = 'PENDING'
          AND (locked_until IS NULL OR locked_until <= :now)
        """, nativeQuery = true)
    int claimEvents(
            @Param("eventIds") List<String> eventIds,
            @Param("token") String token,
            @Param("now") Instant now,
            @Param("lockedUntil") Instant lockedUntil
    );
    @Modifying
    @Query(value = """
    UPDATE order_outbox_events
    SET status = 'PUBLISHED',
        published_at = :publishedAt,
        lock_token = NULL,
        locked_until = NULL,
        last_error = NULL
    WHERE event_id = :eventId
      AND status = 'PENDING'
      AND lock_token = :token
    """, nativeQuery = true)
    int markAsPublished(
            @Param("eventId") String eventId,
            @Param("token") String token,
            @Param("publishedAt") Instant publishedAt
    );

    @Modifying
    @Query(value = """
    UPDATE order_outbox_events
    SET attempts = attempts + 1,
        next_attempt_at = :nextAttemptAt,
        lock_token = NULL,
        locked_until = NULL,
        last_error = :lastError
    WHERE event_id = :eventId
      AND status = 'PENDING'
      AND lock_token = :token
    """, nativeQuery = true)
    int scheduleRetry(
            @Param("eventId") String eventId,
            @Param("token") String token,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError
    );

    @Modifying
    @Query(value = """
    UPDATE order_outbox_events
    SET locked_until = :newLockedUntil
    WHERE event_id = :eventId
      AND status = 'PENDING'
      AND lock_token = :token
      AND locked_until > :now
    """, nativeQuery = true)
    int renewLease(
            @Param("eventId") String eventId,
            @Param("token") String token,
            @Param("now") Instant now,
            @Param("newLockedUntil") Instant newLockedUntil
    );
}