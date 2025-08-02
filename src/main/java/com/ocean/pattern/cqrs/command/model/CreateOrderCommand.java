package com.ocean.pattern.cqrs.command.model;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderCommand {
    private String customerId;
    private List<ProductItem> productItems;
    private String shippingAddress;
}
