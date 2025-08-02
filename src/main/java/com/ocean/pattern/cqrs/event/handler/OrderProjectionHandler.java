package com.ocean.pattern.cqrs.event.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.pattern.cqrs.command.entity.Product;
import com.ocean.pattern.cqrs.command.repository.ProductCommandRepository;
import com.ocean.pattern.cqrs.event.model.OrderCancelledEvent;
import com.ocean.pattern.cqrs.event.model.OrderCreatedEvent;
import com.ocean.pattern.cqrs.event.model.OrderStatusChangedEvent;
import com.ocean.pattern.cqrs.event.store.EventStore;
import com.ocean.pattern.cqrs.query.model.view.OrderItemView;
import com.ocean.pattern.cqrs.query.model.view.OrderReadModel;
import com.ocean.pattern.cqrs.query.model.view.OrderTimelineView;
import com.ocean.pattern.cqrs.query.repository.OrderReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderProjectionHandler {

    private final OrderReadRepository orderReadRepository;
    private final ProductCommandRepository productRepository;
    private final ObjectMapper objectMapper;

    public OrderProjectionHandler(OrderReadRepository orderReadRepository, ProductCommandRepository productRepository, ObjectMapper objectMapper) {
        this.orderReadRepository = orderReadRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void on(EventStore eventStore) {
        try {
            String eventType = eventStore.getEventType();
            if (eventType.equals(OrderCreatedEvent.class.getSimpleName())) {
                handle(objectMapper.readValue(eventStore.getEventData(), OrderCreatedEvent.class));
            } else if (eventType.equals(OrderStatusChangedEvent.class.getSimpleName())) {
                handle(objectMapper.readValue(eventStore.getEventData(), OrderStatusChangedEvent.class));
            } else if (eventType.equals(OrderCancelledEvent.class.getSimpleName())) {
                handle(objectMapper.readValue(eventStore.getEventData(), OrderCancelledEvent.class));
            }
        } catch (Exception e) {
            // Handle exception
        }
    }

    private void handle(OrderCreatedEvent event) {
        try {
            List<OrderItemView> itemViews = event.getProductItems().stream().map(item -> {
                Product product = productRepository.findById(item.getProductId()).orElse(new Product());
                return new OrderItemView(item.getProductId(), product.getName(), item.getQuantity(), product.getPrice());
            }).collect(Collectors.toList());

            OrderReadModel order = new OrderReadModel();
            order.setId(event.getAggregateId());
            order.setCustomerInfo(event.getCustomerId());
            order.setItems(objectMapper.writeValueAsString(itemViews));
            order.setTotalAmount(event.getTotalAmount());
            order.setStatus(event.getStatus());

            List<OrderTimelineView> timeline = new ArrayList<>();
            timeline.add(new OrderTimelineView(event.getStatus(), "Order created", LocalDateTime.now()));
            order.setTimeline(objectMapper.writeValueAsString(timeline));

            orderReadRepository.save(order);
        } catch (Exception e) {
            // Handle JSON serialization error
        }
    }

    private void handle(OrderStatusChangedEvent event) {
        orderReadRepository.findById(event.getAggregateId()).ifPresent(order -> {
            try {
                order.setStatus(event.getNewStatus());
                
                // Deserialize existing timeline, add new entry, serialize back
                List<OrderTimelineView> timeline = objectMapper.readValue(order.getTimeline(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OrderTimelineView.class));
                timeline.add(new OrderTimelineView(event.getNewStatus(), event.getReason(), LocalDateTime.now()));
                order.setTimeline(objectMapper.writeValueAsString(timeline));
                
                orderReadRepository.save(order);
            } catch (Exception e) {
                // Handle JSON serialization error
            }
        });
    }

    private void handle(OrderCancelledEvent event) {
        orderReadRepository.findById(event.getAggregateId()).ifPresent(order -> {
            try {
                order.setStatus("CANCELLED");
                
                // Deserialize existing timeline, add new entry, serialize back
                List<OrderTimelineView> timeline = objectMapper.readValue(order.getTimeline(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OrderTimelineView.class));
                timeline.add(new OrderTimelineView("CANCELLED", event.getReason(), LocalDateTime.now()));
                order.setTimeline(objectMapper.writeValueAsString(timeline));
                
                orderReadRepository.save(order);
            } catch (Exception e) {
                // Handle JSON serialization error
            }
        });
    }
}
