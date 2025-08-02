package com.ocean.pattern.cqrs.query.model.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "cqrs_product_read_model")
@Data
public class ProductReadModel {
    @Id
    private UUID id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String stockStatus;
    private double avgRating;
}
