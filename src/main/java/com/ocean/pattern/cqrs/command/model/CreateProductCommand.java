package com.ocean.pattern.cqrs.command.model;

import lombok.Data;

@Data
public class CreateProductCommand {
    private String name;
    private String description;
    private double price;
    private String categoryId;
    private int stockQuantity;
}
