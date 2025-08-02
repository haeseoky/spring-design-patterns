package com.ocean.pattern.cqrs.query.model.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimelineView {
    private String status;
    private String notes;
    private LocalDateTime timestamp;
}
