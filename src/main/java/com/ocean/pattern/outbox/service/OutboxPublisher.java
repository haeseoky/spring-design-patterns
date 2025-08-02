package com.ocean.pattern.outbox.service;

import com.ocean.pattern.outbox.entity.OutboxEvent;
import com.ocean.pattern.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    
    private final OutboxEventRepository outboxEventRepository;
    private final MessagePublisher messagePublisher;
    
    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    @Async
    public void processOutboxEvents() {
        List<OutboxEvent> unprocessedEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        
        if (!unprocessedEvents.isEmpty()) {
            log.info("Processing {} unprocessed outbox events", unprocessedEvents.size());
            
            for (OutboxEvent event : unprocessedEvents) {
                processEvent(event);
            }
        }
    }
    
    @Transactional
    public void processEvent(OutboxEvent event) {
        try {
            // 메시지 발행 (실제로는 Kafka, RabbitMQ 등의 메시지 브로커로 발행)
            messagePublisher.publish(event);
            
            // 이벤트를 처리 완료로 마킹
            event.setProcessed(true);
            event.setProcessedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
            
            log.info("Successfully processed outbox event: {} for aggregate: {}", 
                    event.getEventType(), event.getAggregateId());
            
        } catch (Exception e) {
            log.error("Failed to process outbox event: {} for aggregate: {}", 
                    event.getEventType(), event.getAggregateId(), e);
            
            // 에러 메시지 저장
            event.setErrorMessage(e.getMessage());
            outboxEventRepository.save(event);
            
            // 재시도 로직을 위해 processed를 false로 유지
        }
    }
    
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시에 실행
    @Transactional
    public void cleanupProcessedEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // 7일 이전 데이터 삭제
        
        try {
            outboxEventRepository.deleteByProcessedTrueAndCreatedAtBefore(cutoffDate);
            log.info("Cleaned up processed outbox events older than {}", cutoffDate);
        } catch (Exception e) {
            log.error("Failed to cleanup processed outbox events", e);
        }
    }
    
    public void publishEventImmediately(OutboxEvent event) {
        processEvent(event);
    }
    
    public long getUnprocessedEventCount() {
        return outboxEventRepository.countByProcessedFalse();
    }
}