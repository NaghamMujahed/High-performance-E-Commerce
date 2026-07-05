package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.exception.InvalidQuantityException;
import com.example.demo.exception.ProductNotFoundException;
import com.example.demo.exception.QuantityNotSufficient;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.model.CustomerOrder;
import com.example.demo.model.OrderItem;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.PaymentStatus;
import com.example.demo.model.Product;
import com.example.demo.repository.CustomerOrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final CustomerOrderRepository orderRepository;
    private final PaymentRepository paymentAttemptRepository;
    private final Clock clock;

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public OrderResponse placeOrder(CreateOrderRequest request) {

        validateRequest(request);

        int updatedRows = productRepository.decreaseStockIfAvailable(
                request.productId(),
                request.quantity());

        if (updatedRows == 0) {
            throw new QuantityNotSufficient();
        }

        Product product = productRepository.findActiveProductById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));

        CustomerOrder order = CustomerOrder.create(
                request.userId(),
                OrderStatus.CREATED,
                LocalDateTime.now(clock));

        OrderItem item = OrderItem.create(
                order,
                product.getId(),
                product.getName(),
                product.getPrice(),
                request.quantity());

        order.addItem(item);
        order.recalculateTotal();

        CustomerOrder savedOrder = orderRepository.save(order);

        PaymentAttempt paymentAttempt = PaymentAttempt.create(
                savedOrder,
                request.paymentMethod(),
                PaymentStatus.INITIATED,
                LocalDateTime.now(clock));

        paymentAttemptRepository.save(paymentAttempt);

        OutboxEvent event = OutboxEvent.orderCreated(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount());

        outboxEventRepository.save(event);

        return OrderMapper.toResponse(savedOrder);
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.quantity() <= 0) {
            throw new InvalidQuantityException(request.quantity());
        }
    }
}