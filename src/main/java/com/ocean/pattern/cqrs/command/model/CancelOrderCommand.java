package com.ocean.pattern.cqrs.command.model;

import lombok.Data;

import java.util.UUID;

@Data
public class CancelOrderCommand {
    private UUID orderId;
    private String reason;
}
