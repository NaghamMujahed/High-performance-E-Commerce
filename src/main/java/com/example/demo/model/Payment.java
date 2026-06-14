package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order", columnList = "order_id"),
                @Index(name = "idx_payments_user", columnList = "user_id"),
                @Index(name = "idx_payments_status", columnList = "status")
        }
)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING)
    private PaymentMethod method = PaymentMethod.CARD;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;
    @Column(precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "transaction_reference")
    private String transactionReference;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public Payment() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (method == null) {
            method = PaymentMethod.CARD;
        }
    }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public User getUser() { return user; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getTransactionReference() { return transactionReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }

    public void setId(Long id) { this.id = id; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public void setUser(User user) { this.user = user; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
