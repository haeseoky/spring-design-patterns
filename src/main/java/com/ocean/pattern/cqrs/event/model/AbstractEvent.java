package com.ocean.pattern.cqrs.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public abstract class AbstractEvent {
    protected final UUID eventId = UUID.randomUUID();
    protected final LocalDateTime timestamp = LocalDateTime.now();
    protected UUID aggregateId;

    public AbstractEvent(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }
}
