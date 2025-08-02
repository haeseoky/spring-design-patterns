package com.ocean.pattern.cqrs.query.model.view;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cqrs_order_read_model")
@Data
public class OrderReadModel {
    @Id
    private UUID id;
    private String customerInfo;

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private List<OrderItemView> items;

    private double totalAmount;
    private String status;

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private List<OrderTimelineView> timeline;
}
