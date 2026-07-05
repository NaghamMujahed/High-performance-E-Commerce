package com.example.demo.dto;

public record CreateOrderRequest(
        Long userId,
        Long productId,
        int quantity,
        String paymentMethod) {
}