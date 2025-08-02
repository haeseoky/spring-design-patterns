package com.ocean.pattern.cqrs.command.repository;

import com.ocean.pattern.cqrs.command.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductCommandRepository extends JpaRepository<Product, UUID> {
}
