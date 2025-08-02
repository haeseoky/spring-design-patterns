package com.ocean.pattern.cqrs.event.model;

import com.ocean.pattern.cqrs.command.model.ProductItem;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class OrderCreatedEvent extends AbstractEvent {
    private String customerId;
    private List<ProductItem> productItems;
    private double totalAmount;
    private String shippingAddress;
    private String status;

    public OrderCreatedEvent(UUID aggregateId, String customerId, List<ProductItem> productItems, double totalAmount, String shippingAddress, String status) {
        super(aggregateId);
        this.customerId = customerId;
        this.productItems = productItems;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.status = status;
    }
}
