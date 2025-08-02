package com.ocean.pattern.outbox.service;

import com.ocean.pattern.outbox.entity.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessagePublisher {
    
    public void publish(OutboxEvent event) {
        // 실제 구현에서는 Kafka, RabbitMQ, AWS SQS 등의 메시지 브로커를 사용
        // 여기서는 로깅으로 대체
        
        log.info("📧 Publishing message to external system:");
        log.info("  Event Type: {}", event.getEventType());
        log.info("  Aggregate ID: {}", event.getAggregateId());
        log.info("  Aggregate Type: {}", event.getAggregateType());
        log.info("  Event Data: {}", event.getEventData());
        log.info("  Created At: {}", event.getCreatedAt());
        
        // 메시지 발행 시뮬레이션
        simulateMessageBroker(event);
        
        log.info("✅ Message published successfully for event: {}", event.getId());
    }
    
    private void simulateMessageBroker(OutboxEvent event) {
        // 실제 메시지 브로커 연동 시뮬레이션
        // 예: 주문 생성 이벤트 처리
        switch (event.getEventType()) {
            case "ORDER_CREATED":
                log.info("🛒 Sending order confirmation email...");
                log.info("📦 Notifying inventory service...");
                break;
            case "ORDER_CONFIRMED":
                log.info("✅ Sending order confirmation notification...");
                log.info("🚚 Starting shipping process...");
                break;
            case "ORDER_CANCELLED":
                log.info("❌ Sending cancellation notification...");
                log.info("📦 Restoring inventory...");
                break;
            default:
                log.info("🔄 Processing event: {}", event.getEventType());
        }
        
        // 네트워크 지연 시뮬레이션
        try {
            Thread.sleep(100); // 100ms 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Message publishing interrupted", e);
        }
    }
}