package com.tradeflow.tradeflow_order_service.service;

import com.tradeflow.tradeflow_order_service.entity.OutboxEvent;
import com.tradeflow.tradeflow_order_service.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxClaimService {

    private static final int BATCH_SIZE = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final OutboxEventRepository repository;

    public OutboxClaimService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<OutboxEvent> claimPendingEvents() {

        Instant now = Instant.now();

        List<String> eventIds =
                repository.findClaimableEventIds(now, BATCH_SIZE);

        if (eventIds.isEmpty()) {
            return List.of();
        }

        String token = UUID.randomUUID().toString();
        Instant lockedUntil = now.plus(LOCK_DURATION);

        int claimedCount = repository.claimEvents(
                eventIds,
                token,
                now,
                lockedUntil
        );

        if (claimedCount != eventIds.size()) {
            throw new IllegalStateException(
                    "Unexpected outbox claim count. Expected "
                            + eventIds.size()
                            + " but claimed "
                            + claimedCount
            );
        }

        return repository.findAllById(eventIds)
                .stream()
                .filter(event -> token.equals(event.getLockToken()))
                .toList();
    }
}