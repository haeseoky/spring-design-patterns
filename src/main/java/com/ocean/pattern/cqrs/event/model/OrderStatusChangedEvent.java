package com.ocean.pattern.cqrs.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class OrderStatusChangedEvent extends AbstractEvent {
    private String newStatus;
    private String reason;

    public OrderStatusChangedEvent(UUID aggregateId, String newStatus, String reason) {
        super(aggregateId);
        this.newStatus = newStatus;
        this.reason = reason;
    }
}
