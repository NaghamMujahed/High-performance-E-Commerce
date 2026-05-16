package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ===== Notification =====
    public static final String NOTIFICATION_QUEUE    = "notification.queue";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_KEY      = "notification.key";

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_KEY);
    }

    // ===== Payment =====
    public static final String PAYMENT_QUEUE    = "payment.queue";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_KEY      = "payment.key";

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(paymentExchange())
                .with(PAYMENT_KEY);
    }

    // ===== Shared =====
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}