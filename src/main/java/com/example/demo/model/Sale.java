package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "sales")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private int quantity;
    private double totalPrice;
    private LocalDate saleDate;
    private boolean processed;

    public Sale() {}

    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }
    public LocalDate getSaleDate() { return saleDate; }
    public boolean isProcessed() { return processed; }

    public void setId(Long id) { this.id = id; }
    public void setProductName(String name) { this.productName = name; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setTotalPrice(double price) { this.totalPrice = price; }
    public void setSaleDate(LocalDate date) { this.saleDate = date; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}