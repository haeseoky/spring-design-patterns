package com.ocean.pattern.cqrs.command.controller;

import com.ocean.pattern.cqrs.command.handler.ProductCommandHandler;
import com.ocean.pattern.cqrs.command.model.CreateProductCommand;
import com.ocean.pattern.cqrs.command.model.UpdateProductCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cqrs/products/commands")
public class ProductCommandController {

    private final ProductCommandHandler commandHandler;

    public ProductCommandController(ProductCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping
    public ResponseEntity<UUID> createProduct(@RequestBody CreateProductCommand command) {
        UUID productId = commandHandler.handle(command);
        return new ResponseEntity<>(productId, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<Void> updateProduct(@RequestBody UpdateProductCommand command) {
        commandHandler.handle(command);
        return ResponseEntity.ok().build();
    }
}
