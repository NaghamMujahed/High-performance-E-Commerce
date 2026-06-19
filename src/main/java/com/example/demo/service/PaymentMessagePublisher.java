package com.example.demo.service;

import com.example.demo.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PaymentMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async("paymentPublishExecutor")
    public void publishPaymentTask(Long userId, double amount) {
        try {
            logger.info("[PAYMENT-QUEUE] Sending payment task for userId: {} | Amount: {}", userId, amount);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_KEY,
                    userId + ":" + amount);
        } catch (AmqpException ex) {
            logger.error("[PAYMENT-QUEUE] Failed to publish payment task for userId: {}", userId, ex);
        }
    }
}
