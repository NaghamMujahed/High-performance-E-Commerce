package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order", columnList = "order_id"),
                @Index(name = "idx_order_items_product", columnList = "product_id")
        }
)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private int quantity;
    @Column(name = "line_total", precision = 12, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public OrderItem() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public Product getProduct() { return product; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public void setProduct(Product product) { this.product = product; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
