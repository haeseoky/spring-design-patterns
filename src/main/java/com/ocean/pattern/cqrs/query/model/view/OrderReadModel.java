package com.ocean.pattern.cqrs.query.model.view;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "cqrs_order_read_model")
@Data
public class OrderReadModel {
    @Id
    private UUID id;
    private String customerInfo;

    @Column(columnDefinition = "TEXT")
    private String items;

    private double totalAmount;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String timeline;
}
