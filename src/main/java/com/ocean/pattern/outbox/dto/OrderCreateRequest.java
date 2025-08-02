package com.ocean.pattern.outbox.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreateRequest {
    private String customerName;
    private String customerEmail;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}