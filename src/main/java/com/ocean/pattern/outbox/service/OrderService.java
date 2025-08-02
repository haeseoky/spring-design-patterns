package com.ocean.pattern.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.pattern.outbox.dto.OrderCreateRequest;
import com.ocean.pattern.outbox.entity.Order;
import com.ocean.pattern.outbox.entity.OutboxEvent;
import com.ocean.pattern.outbox.repository.OrderRepository;
import com.ocean.pattern.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public Order createOrder(OrderCreateRequest request) {
        // 1. 주문 생성
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .status(Order.OrderStatus.PENDING)
                .build();
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created: {}", savedOrder.getOrderNumber());
        
        // 2. 아웃박스 이벤트 생성 (같은 트랜잭션 내에서)
        createOutboxEvent(savedOrder, "ORDER_CREATED");
        
        return savedOrder;
    }
    
    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Order cannot be confirmed. Current status: " + order.getStatus());
        }
        
        order.setStatus(Order.OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);
        
        // 아웃박스 이벤트 생성
        createOutboxEvent(savedOrder, "ORDER_CONFIRMED");
        
        log.info("Order confirmed: {}", savedOrder.getOrderNumber());
        return savedOrder;
    }
    
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel delivered order");
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        
        // 아웃박스 이벤트 생성
        createOutboxEvent(savedOrder, "ORDER_CANCELLED");
        
        log.info("Order cancelled: {}", savedOrder.getOrderNumber());
        return savedOrder;
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }
    
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
    }
    
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
    private void createOutboxEvent(Order order, String eventType) {
        try {
            String eventData = objectMapper.writeValueAsString(order);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(order.getId().toString())
                    .aggregateType("Order")
                    .eventType(eventType)
                    .eventData(eventData)
                    .build();
            
            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event created: {} for order {}", eventType, order.getOrderNumber());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to create outbox event for order {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
    
    private String generateOrderNumber() {
        return "ORDER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}