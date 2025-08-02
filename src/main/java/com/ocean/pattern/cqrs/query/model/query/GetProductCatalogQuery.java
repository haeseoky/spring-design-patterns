package com.ocean.pattern.cqrs.query.model.query;

import lombok.Data;

@Data
public class GetProductCatalogQuery {
    private String category;
    private Double minPrice;
    private Double maxPrice;
    private Boolean inStock;
}
