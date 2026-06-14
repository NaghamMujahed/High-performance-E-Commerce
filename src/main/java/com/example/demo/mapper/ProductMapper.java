package com.example.demo.mapper;

import com.example.demo.model.Product;
import com.example.demo.model.ProductStatus;
import com.example.demo.dto.ProductDetailsResponse;

public final class ProductMapper {
    public ProductMapper() {
    }

    public static ProductDetailsResponse toDetailsRecord(Product product) {
        return new ProductDetailsResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getStatus().equals(ProductStatus.ACTIVE) ? true : false);
    }
}
