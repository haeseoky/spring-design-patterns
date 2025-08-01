package com.ocean.pattern.cqrs.event.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventStore, UUID> {
    List<EventStore> findByProcessedFalseOrderByTimestampAsc();
}
