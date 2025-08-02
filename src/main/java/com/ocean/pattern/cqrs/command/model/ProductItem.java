package com.ocean.pattern.cqrs.command.model;

import lombok.Data;

import java.util.UUID;

@Data
public class ProductItem {
    private UUID productId;
    private int quantity;
}
