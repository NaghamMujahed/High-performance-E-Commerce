package com.example.demo.mapper;

import com.example.demo.model.CustomerOrder;
import com.example.demo.dto.OrderResponse;

public final class OrderMapper {
    public OrderMapper() {
    }

    public static OrderResponse toResponse(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus(),
                order.getTotalAmount());
    }
}
