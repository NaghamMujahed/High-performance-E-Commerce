package com.example.demo.dto;

import java.math.BigDecimal;

import com.example.demo.model.OrderStatus;

public record OrderResponse(
        Long orderId,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount) {
}