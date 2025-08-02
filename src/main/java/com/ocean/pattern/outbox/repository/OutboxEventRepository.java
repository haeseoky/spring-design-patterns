package com.ocean.pattern.outbox.repository;

import com.ocean.pattern.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
    
    List<OutboxEvent> findByAggregateIdAndAggregateType(String aggregateId, String aggregateType);
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.createdAt < :before")
    List<OutboxEvent> findUnprocessedEventsBefore(LocalDateTime before);
    
    List<OutboxEvent> findByEventTypeAndProcessedFalse(String eventType);
    
    long countByProcessedFalse();
    
    void deleteByProcessedTrueAndCreatedAtBefore(LocalDateTime before);
}