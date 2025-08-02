package com.ocean.pattern.cqrs.query.model.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemView {
    private UUID productId;
    private String productName;
    private int quantity;
    private double unitPrice;
}
