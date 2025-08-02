package com.ocean.pattern.cqrs.query.handler;

import com.ocean.pattern.cqrs.query.model.query.GetProductCatalogQuery;
import com.ocean.pattern.cqrs.query.model.view.ProductReadModel;
import com.ocean.pattern.cqrs.query.repository.ProductReadRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductQueryHandler {

    private final ProductReadRepository productRepository;

    public ProductQueryHandler(ProductReadRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductReadModel> handle(GetProductCatalogQuery query) {
        return productRepository.findAll((Specification<ProductReadModel>) (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getCategory() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), query.getCategory()));
            }
            if (query.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), query.getMinPrice()));
            }
            if (query.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), query.getMaxPrice()));
            }
            if (query.getInStock() != null && query.getInStock()) {
                predicates.add(criteriaBuilder.notEqual(root.get("stockStatus"), "OUT_OF_STOCK"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }

    public ProductReadModel handle(UUID productId) {
        return productRepository.findById(productId).orElse(null);
    }
}
