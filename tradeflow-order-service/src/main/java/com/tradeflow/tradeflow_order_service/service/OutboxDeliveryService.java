package com.tradeflow.tradeflow_order_service.service;

import com.tradeflow.tradeflow_order_service.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class OutboxDeliveryService {

    private final OutboxEventRepository repository;

    public OutboxDeliveryService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean markAsPublished(
            String eventId,
            String token
    ) {
        return repository.markAsPublished(
                eventId,
                token,
                Instant.now()
        ) == 1;
    }

    @Transactional
    public boolean scheduleRetry(
            String eventId,
            String token,
            Instant nextAttemptAt,
            String errorMessage
    ) {
        return repository.scheduleRetry(
                eventId,
                token,
                nextAttemptAt,
                truncateError(errorMessage)
        ) == 1;
    }

    private String truncateError(String message) {
        if (message == null) {
            return "Unknown publishing error";
        }

        // Keep diagnostic data bounded.
        int maxLength = 2000;

        return message.length() <= maxLength
                ? message
                : message.substring(0, maxLength);
    }

    @Transactional
    public boolean renewLease(
            String eventId,
            String token
    ) {
        Instant now = Instant.now();

        Instant newLockedUntil = now.plus(Duration.ofMinutes(5));

        return repository.renewLease(
                eventId,
                token,
                now,
                newLockedUntil
        ) == 1;
    }
}