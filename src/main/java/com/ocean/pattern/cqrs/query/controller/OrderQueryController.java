package com.ocean.pattern.cqrs.query.controller;

import com.ocean.pattern.cqrs.query.handler.OrderQueryHandler;
import com.ocean.pattern.cqrs.query.model.query.GetOrderHistoryQuery;
import com.ocean.pattern.cqrs.query.model.view.OrderReadModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cqrs/orders/queries")
public class OrderQueryController {

    private final OrderQueryHandler queryHandler;

    public OrderQueryController(OrderQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderReadModel> getOrder(@PathVariable UUID id) {
        OrderReadModel order = queryHandler.handle(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderReadModel>> getOrderHistory(GetOrderHistoryQuery query) {
        List<OrderReadModel> orders = queryHandler.handle(query);
        return ResponseEntity.ok(orders);
    }
}
