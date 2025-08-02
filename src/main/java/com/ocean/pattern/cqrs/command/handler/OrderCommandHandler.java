package com.ocean.pattern.cqrs.command.handler;

import com.ocean.pattern.cqrs.command.entity.Order;
import com.ocean.pattern.cqrs.command.entity.OrderItem;
import com.ocean.pattern.cqrs.command.entity.Product;
import com.ocean.pattern.cqrs.command.model.CancelOrderCommand;
import com.ocean.pattern.cqrs.command.model.CreateOrderCommand;
import com.ocean.pattern.cqrs.command.model.UpdateOrderStatusCommand;
import com.ocean.pattern.cqrs.command.repository.OrderCommandRepository;
import com.ocean.pattern.cqrs.command.repository.ProductCommandRepository;
import com.ocean.pattern.cqrs.common.exception.OrderNotFoundException;
import com.ocean.pattern.cqrs.common.exception.ProductNotFoundException;
import com.ocean.pattern.cqrs.event.model.OrderCancelledEvent;
import com.ocean.pattern.cqrs.event.model.OrderCreatedEvent;
import com.ocean.pattern.cqrs.event.model.OrderStatusChangedEvent;
import com.ocean.pattern.cqrs.event.store.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderCommandHandler {

    private final OrderCommandRepository orderRepository;
    private final ProductCommandRepository productRepository;
    private final EventPublisher eventPublisher;

    public OrderCommandHandler(OrderCommandRepository orderRepository, ProductCommandRepository productRepository, EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID handle(CreateOrderCommand command) {
        List<OrderItem> orderItems = command.getProductItems().stream().map(item -> {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));
            product.decreaseStock(item.getQuantity());
            productRepository.save(product);
            return OrderItem.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
        }).collect(Collectors.toList());

        double totalAmount = orderItems.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        Order order = Order.builder()
                .customerId(command.getCustomerId())
                .shippingAddress(command.getShippingAddress())
                .items(orderItems)
                .totalAmount(totalAmount)
                .status("CREATED")
                .build();

        orderItems.forEach(item -> item.setOrder(order));

        orderRepository.save(order);

        eventPublisher.publish(new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                command.getProductItems(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                order.getStatus()
        ));

        return order.getId();
    }

    @Transactional
    public void handle(UpdateOrderStatusCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        order.updateStatus(command.getNewStatus());
        orderRepository.save(order);

        eventPublisher.publish(new OrderStatusChangedEvent(
                order.getId(),
                command.getNewStatus(),
                command.getReason()
        ));
    }

    @Transactional
    public void handle(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        order.updateStatus("CANCELLED");
        orderRepository.save(order);

        eventPublisher.publish(new OrderCancelledEvent(
                order.getId(),
                command.getReason()
        ));
    }
}
