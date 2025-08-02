package com.ocean.pattern.cqrs.event.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.pattern.cqrs.event.model.AbstractEvent;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventPublisher(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public void publish(AbstractEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            EventStore eventStore = EventStore.builder()
                    .aggregateId(event.getAggregateId())
                    .aggregateType(event.getClass().getSimpleName().replace("Event", ""))
                    .eventType(event.getClass().getSimpleName())
                    .eventData(eventData)
                    .build();
            eventRepository.save(eventStore);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing event", e);
        }
    }
}
