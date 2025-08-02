package com.ocean.pattern.cqrs.command.controller;

import com.ocean.pattern.cqrs.command.handler.OrderCommandHandler;
import com.ocean.pattern.cqrs.command.model.CancelOrderCommand;
import com.ocean.pattern.cqrs.command.model.CreateOrderCommand;
import com.ocean.pattern.cqrs.command.model.UpdateOrderStatusCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cqrs/orders/commands")
public class OrderCommandController {

    private final OrderCommandHandler commandHandler;

    public OrderCommandController(OrderCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping
    public ResponseEntity<UUID> createOrder(@RequestBody CreateOrderCommand command) {
        UUID orderId = commandHandler.handle(command);
        return new ResponseEntity<>(orderId, HttpStatus.CREATED);
    }

    @PutMapping("/status")
    public ResponseEntity<Void> updateOrderStatus(@RequestBody UpdateOrderStatusCommand command) {
        commandHandler.handle(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelOrder(@RequestBody CancelOrderCommand command) {
        commandHandler.handle(command);
        return ResponseEntity.ok().build();
    }
}
