package com.ocean.pattern.cqrs.command.repository;

import com.ocean.pattern.cqrs.command.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderCommandRepository extends JpaRepository<Order, UUID> {
}
