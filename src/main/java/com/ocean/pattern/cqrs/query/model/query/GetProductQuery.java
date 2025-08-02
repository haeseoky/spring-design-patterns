package com.ocean.pattern.cqrs.query.model.query;

import lombok.Data;

import java.util.UUID;

@Data
public class GetProductQuery {
    private UUID productId;
}
