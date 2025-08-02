package com.ocean.pattern.cqrs.query.repository;

import com.ocean.pattern.cqrs.query.model.view.OrderReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderReadRepository extends JpaRepository<OrderReadModel, UUID> {
    List<OrderReadModel> findByCustomerInfo(String customerInfo);
}
