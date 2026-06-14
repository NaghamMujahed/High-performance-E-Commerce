package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_transactions",
        indexes = {
                @Index(name = "idx_inventory_product", columnList = "product_id"),
                @Index(name = "idx_inventory_type", columnList = "type"),
                @Index(name = "idx_inventory_created_at", columnList = "created_at")
        }
)
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    @Enumerated(EnumType.STRING)
    private InventoryTransactionType type = InventoryTransactionType.ADJUSTMENT;
    @Column(name = "quantity_change")
    private int quantityChange;
    @Column(name = "stock_after")
    private int stockAfter;
    @Column(name = "reference_type")
    private String referenceType;
    @Column(name = "reference_id")
    private Long referenceId;
    private String reason;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public InventoryTransaction() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (type == null) {
            type = InventoryTransactionType.ADJUSTMENT;
        }
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public InventoryTransactionType getType() { return type; }
    public int getQuantityChange() { return quantityChange; }
    public int getStockAfter() { return stockAfter; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setProduct(Product product) { this.product = product; }
    public void setType(InventoryTransactionType type) { this.type = type; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
    public void setStockAfter(int stockAfter) { this.stockAfter = stockAfter; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
