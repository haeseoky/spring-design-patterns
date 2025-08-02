package com.ocean.pattern.cqrs.query.model.query;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GetOrderHistoryQuery {
    private String customerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
