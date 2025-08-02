package com.ocean.pattern.outbox.repository;

import com.ocean.pattern.outbox.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    List<Order> findByCustomerEmail(String customerEmail);
    
    boolean existsByOrderNumber(String orderNumber);
}