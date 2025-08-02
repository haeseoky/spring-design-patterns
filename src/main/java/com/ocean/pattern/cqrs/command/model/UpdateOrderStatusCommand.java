package com.ocean.pattern.cqrs.command.model;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateOrderStatusCommand {
    private UUID orderId;
    private String newStatus;
    private String reason;
}
