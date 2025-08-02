package com.ocean.pattern.cqrs.query.controller;

import com.ocean.pattern.cqrs.query.handler.ProductQueryHandler;
import com.ocean.pattern.cqrs.query.model.query.GetProductCatalogQuery;
import com.ocean.pattern.cqrs.query.model.view.ProductReadModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cqrs/products/queries")
public class ProductQueryController {

    private final ProductQueryHandler queryHandler;

    public ProductQueryController(ProductQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductReadModel> getProduct(@PathVariable UUID id) {
        ProductReadModel product = queryHandler.handle(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<ProductReadModel>> getProductCatalog(GetProductCatalogQuery query) {
        List<ProductReadModel> products = queryHandler.handle(query);
        return ResponseEntity.ok(products);
    }
}
