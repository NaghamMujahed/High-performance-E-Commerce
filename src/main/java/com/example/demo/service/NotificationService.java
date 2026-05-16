package com.example.demo.service;

import com.example.demo.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(NotificationService.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificationService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendEmailSync(String customerName) {
        try {
            logger.info("[SYNC] Started sending email to: {}", customerName);
            Thread.sleep(3000);
            logger.info("[SYNC] Email sent successfully to: {}", customerName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[SYNC] Failed sending email to: {}", customerName, e);
        }
    }

    public void sendEmailAsync(String customerName) {
        logger.info("[QUEUE] Sending notification to queue for: {}", customerName);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_KEY,
                customerName
        );
        logger.info("[QUEUE] Message sent successfully for: {}", customerName);
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE , concurrency = "3-10")
    public void handleNotification(String customerName) {
        try {
            logger.info("[CONSUMER] Received notification for: {}", customerName);
            Thread.sleep(3000);
            logger.info("[CONSUMER] Email processed successfully for: {}", customerName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[CONSUMER] Failed processing notification for: {}", customerName, e);
        }
    }
}