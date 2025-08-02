package com.ocean.pattern.cqrs.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class OrderCancelledEvent extends AbstractEvent {
    private String reason;

    public OrderCancelledEvent(UUID aggregateId, String reason) {
        super(aggregateId);
        this.reason = reason;
    }
}
