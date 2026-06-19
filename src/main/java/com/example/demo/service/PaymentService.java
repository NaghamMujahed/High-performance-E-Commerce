package com.example.demo.service;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.exception.BalanceNotFound;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final UserRepository userRepository;
    private final PaymentMessagePublisher paymentMessagePublisher;

    public PaymentService(UserRepository userRepository, PaymentMessagePublisher paymentMessagePublisher) {
        this.userRepository = userRepository;
        this.paymentMessagePublisher = paymentMessagePublisher;
    }

    @Transactional
    public boolean processPaymentSync(Long userId, double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found"));
        if (user.getBalance() < amount) {
            throw new BalanceNotFound();
        }
        try {
            logger.info("[PAYMENT-SYNC] Processing payment for: {} | Amount: {}",
                    user.getName(), amount);
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);
        logger.info("[PAYMENT-SYNC] Payment approved for: {} | New Balance: {}",
                user.getName(), user.getBalance());
        return true;
    }

    @Transactional
    public void processPaymentAsync(Long userId, double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found"));
        if (user.getBalance() < amount) {
            throw new BalanceNotFound();
        }
        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);

        publishPaymentAfterCommit(userId, amount);
    }

    private void publishPaymentAfterCommit(Long userId, double amount) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    paymentMessagePublisher.publishPaymentTask(userId, amount);
                }
            });
            return;
        }

        paymentMessagePublisher.publishPaymentTask(userId, amount);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE, concurrency = "3-10")
    public void handlePayment(String message) {
        try {
            String[] parts = message.split(":");
            Long userId = Long.parseLong(parts[0]);
            double amount = Double.parseDouble(parts[1]);

            logger.info("[PAYMENT-CONSUMER] Confirming payment | Amount: {}", amount);
            Thread.sleep(3000);
            logger.info("[PAYMENT-CONSUMER] Payment confirmed for userId: {}", userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
