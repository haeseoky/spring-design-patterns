package com.ocean.pattern.cqrs.command.model;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateProductCommand {
    private UUID productId;
    private String name;
    private String description;
    private double price;
    private int stockQuantity;
}
