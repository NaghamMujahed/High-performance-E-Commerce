package com.example.demo.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductDetailsResponse(
                Long id,
                String name,
                String description,
                double price,
                String categoryName,
                boolean active) implements Serializable {
}
