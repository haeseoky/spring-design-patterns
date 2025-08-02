package com.ocean.pattern.cqrs.event.handler;

import com.ocean.pattern.cqrs.event.store.EventRepository;
import com.ocean.pattern.cqrs.event.store.EventStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class EventProcessingScheduler {

    private final EventRepository eventRepository;
    private final ProductProjectionHandler productProjectionHandler;
    private final OrderProjectionHandler orderProjectionHandler;

    public EventProcessingScheduler(EventRepository eventRepository, ProductProjectionHandler productProjectionHandler, OrderProjectionHandler orderProjectionHandler) {
        this.eventRepository = eventRepository;
        this.productProjectionHandler = productProjectionHandler;
        this.orderProjectionHandler = orderProjectionHandler;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processEvents() {
        List<EventStore> events = eventRepository.findByProcessedFalseOrderByTimestampAsc();
        for (EventStore event : events) {
            if ("Product".equals(event.getAggregateType())) {
                productProjectionHandler.on(event);
            } else if ("Order".equals(event.getAggregateType())) {
                orderProjectionHandler.on(event);
            }
            event.markAsProcessed();
            eventRepository.save(event);
        }
    }
}
