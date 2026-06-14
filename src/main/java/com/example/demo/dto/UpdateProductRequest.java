package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Product name is required") String name,

        @NotBlank(message = "Product description is required") String description,

        @NotNull(message = "Product price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero") double price,

        Long categoryId,

        Boolean active) {
}