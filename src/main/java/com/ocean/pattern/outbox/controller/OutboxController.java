package com.ocean.pattern.outbox.controller;

import com.ocean.pattern.outbox.dto.OrderCreateRequest;
import com.ocean.pattern.outbox.entity.Order;
import com.ocean.pattern.outbox.entity.OutboxEvent;
import com.ocean.pattern.outbox.repository.OutboxEventRepository;
import com.ocean.pattern.outbox.service.OrderService;
import com.ocean.pattern.outbox.service.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outbox")
@RequiredArgsConstructor
public class OutboxController {
    
    private final OrderService orderService;
    private final OutboxPublisher outboxPublisher;
    private final OutboxEventRepository outboxEventRepository;
    
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderCreateRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }
    
    @PutMapping("/orders/{orderId}/confirm")
    public ResponseEntity<Order> confirmOrder(@PathVariable Long orderId) {
        Order order = orderService.confirmOrder(orderId);
        return ResponseEntity.ok(order);
    }
    
    @PutMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long orderId) {
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        Order order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        List<Order> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/events")
    public ResponseEntity<List<OutboxEvent>> getAllOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findAll();
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/unprocessed")
    public ResponseEntity<List<OutboxEvent>> getUnprocessedEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        return ResponseEntity.ok(events);
    }
    
    @PostMapping("/events/{eventId}/process")
    public ResponseEntity<String> processEvent(@PathVariable Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        
        outboxPublisher.publishEventImmediately(event);
        return ResponseEntity.ok("Event processed successfully");
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOutboxStats() {
        long totalEvents = outboxEventRepository.count();
        long unprocessedEvents = outboxPublisher.getUnprocessedEventCount();
        long processedEvents = totalEvents - unprocessedEvents;
        
        Map<String, Object> stats = Map.of(
                "totalEvents", totalEvents,
                "processedEvents", processedEvents,
                "unprocessedEvents", unprocessedEvents,
                "processedPercentage", totalEvents > 0 ? (processedEvents * 100.0 / totalEvents) : 0
        );
        
        return ResponseEntity.ok(stats);
    }
}