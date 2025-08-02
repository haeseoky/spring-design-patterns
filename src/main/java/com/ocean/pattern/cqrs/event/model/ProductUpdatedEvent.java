package com.ocean.pattern.cqrs.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ProductUpdatedEvent extends AbstractEvent {
    private String name;
    private String description;
    private double price;
    private int stockQuantity;

    public ProductUpdatedEvent(UUID aggregateId, String name, String description, double price, int stockQuantity) {
        super(aggregateId);
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }
}
