package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "sales",
        indexes = {
                @Index(name = "idx_sales_processed_id", columnList = "processed,id"),
                @Index(name = "idx_sales_sale_date", columnList = "sale_date"),
                @Index(name = "idx_sales_product_id", columnList = "product_id"),
                @Index(name = "idx_sales_user_id", columnList = "user_id")
        }
)
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_id")
    private Long productId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "order_number")
    private String orderNumber;
    private String productName;
    private int quantity;
    private double totalPrice;
    @Column(name = "sale_date")
    private LocalDate saleDate;
    private boolean processed;

    public Sale() {}

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getUserId() { return userId; }
    public String getOrderNumber() { return orderNumber; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }
    public LocalDate getSaleDate() { return saleDate; }
    public boolean isProcessed() { return processed; }

    public void setId(Long id) { this.id = id; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public void setProductName(String name) { this.productName = name; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setTotalPrice(double price) { this.totalPrice = price; }
    public void setSaleDate(LocalDate date) { this.saleDate = date; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}
