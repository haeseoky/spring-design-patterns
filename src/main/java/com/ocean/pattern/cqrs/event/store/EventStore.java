package com.ocean.pattern.cqrs.event.store;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cqrs_event_store")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStore {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;

    private UUID aggregateId;
    private String aggregateType;
    private String eventType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String eventData;

    @CreationTimestamp
    private LocalDateTime timestamp;

    @Builder.Default
    private boolean processed = false;

    public void markAsProcessed() {
        this.processed = true;
    }
}
