package com.ocean.pattern.cqrs.query.repository;

import com.ocean.pattern.cqrs.query.model.view.ProductReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductReadRepository extends JpaRepository<ProductReadModel, UUID>, JpaSpecificationExecutor<ProductReadModel> {
}
