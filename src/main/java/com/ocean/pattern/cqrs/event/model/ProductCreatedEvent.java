package com.ocean.pattern.cqrs.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ProductCreatedEvent extends AbstractEvent {
    private String name;
    private String description;
    private double price;
    private String categoryId;
    private int stockQuantity;

    public ProductCreatedEvent(UUID aggregateId, String name, String description, double price, String categoryId, int stockQuantity) {
        super(aggregateId);
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.stockQuantity = stockQuantity;
    }
}
