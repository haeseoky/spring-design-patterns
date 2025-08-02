package com.ocean.pattern.cqrs.query.handler;

import com.ocean.pattern.cqrs.query.model.query.GetOrderHistoryQuery;
import com.ocean.pattern.cqrs.query.model.view.OrderReadModel;
import com.ocean.pattern.cqrs.query.repository.OrderReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderQueryHandler {

    private final OrderReadRepository orderRepository;

    public OrderQueryHandler(OrderReadRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderReadModel handle(UUID orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    public List<OrderReadModel> handle(GetOrderHistoryQuery query) {
        // In a real application, you would build a dynamic query based on the query parameters.
        // For simplicity, we'll just query by customerId here.
        return orderRepository.findByCustomerInfo(query.getCustomerId());
    }
}
