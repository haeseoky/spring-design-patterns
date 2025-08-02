package com.ocean.pattern.cqrs.command.handler;

import com.ocean.pattern.cqrs.command.entity.Product;
import com.ocean.pattern.cqrs.command.model.CreateProductCommand;
import com.ocean.pattern.cqrs.command.model.UpdateProductCommand;
import com.ocean.pattern.cqrs.command.repository.ProductCommandRepository;
import com.ocean.pattern.cqrs.common.exception.ProductNotFoundException;
import com.ocean.pattern.cqrs.event.model.ProductCreatedEvent;
import com.ocean.pattern.cqrs.event.model.ProductUpdatedEvent;
import com.ocean.pattern.cqrs.event.store.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductCommandHandler {

    private final ProductCommandRepository productRepository;
    private final EventPublisher eventPublisher;

    public ProductCommandHandler(ProductCommandRepository productRepository, EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID handle(CreateProductCommand command) {
        Product product = Product.builder()
                .name(command.getName())
                .description(command.getDescription())
                .price(command.getPrice())
                .categoryId(command.getCategoryId())
                .stockQuantity(command.getStockQuantity())
                .build();
        productRepository.save(product);

        eventPublisher.publish(new ProductCreatedEvent(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategoryId(),
                product.getStockQuantity()
        ));

        return product.getId();
    }

    @Transactional
    public void handle(UpdateProductCommand command) {
        Product product = productRepository.findById(command.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        product.update(
                command.getName(),
                command.getDescription(),
                command.getPrice(),
                command.getStockQuantity()
        );
        productRepository.save(product);

        eventPublisher.publish(new ProductUpdatedEvent(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity()
        ));
    }
}
