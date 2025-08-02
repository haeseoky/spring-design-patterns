package com.ocean.pattern.cqrs.event.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.pattern.cqrs.event.model.ProductCreatedEvent;
import com.ocean.pattern.cqrs.event.model.ProductUpdatedEvent;
import com.ocean.pattern.cqrs.event.store.EventStore;
import com.ocean.pattern.cqrs.query.model.view.ProductReadModel;
import com.ocean.pattern.cqrs.query.repository.ProductReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductProjectionHandler {

    private final ProductReadRepository productReadRepository;
    private final ObjectMapper objectMapper;

    public ProductProjectionHandler(ProductReadRepository productReadRepository, ObjectMapper objectMapper) {
        this.productReadRepository = productReadRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void on(EventStore eventStore) {
        try {
            String eventType = eventStore.getEventType();
            if (eventType.equals(ProductCreatedEvent.class.getSimpleName())) {
                handle(objectMapper.readValue(eventStore.getEventData(), ProductCreatedEvent.class));
            } else if (eventType.equals(ProductUpdatedEvent.class.getSimpleName())) {
                handle(objectMapper.readValue(eventStore.getEventData(), ProductUpdatedEvent.class));
            }
        } catch (Exception e) {
            // Handle exception
        }
    }

    private void handle(ProductCreatedEvent event) {
        ProductReadModel product = new ProductReadModel();
        product.setId(event.getAggregateId());
        product.setName(event.getName());
        product.setDescription(event.getDescription());
        product.setPrice(event.getPrice());
        product.setCategory(event.getCategoryId());
        product.setStockStatus(event.getStockQuantity() > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
        product.setAvgRating(0.0); // Default value
        productReadRepository.save(product);
    }

    private void handle(ProductUpdatedEvent event) {
        productReadRepository.findById(event.getAggregateId()).ifPresent(product -> {
            product.setName(event.getName());
            product.setDescription(event.getDescription());
            product.setPrice(event.getPrice());
            product.setStockStatus(event.getStockQuantity() > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
            productReadRepository.save(product);
        });
    }
}
