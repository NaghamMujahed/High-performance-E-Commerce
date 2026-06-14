package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_reviews",
        indexes = {
                @Index(name = "idx_reviews_product", columnList = "product_id"),
                @Index(name = "idx_reviews_user", columnList = "user_id"),
                @Index(name = "idx_reviews_rating", columnList = "rating")
        }
)
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    private int rating;
    private String title;
    @Column(length = 1000)
    private String comment;
    private boolean approved = true;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ProductReview() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public User getUser() { return user; }
    public int getRating() { return rating; }
    public String getTitle() { return title; }
    public String getComment() { return comment; }
    public boolean isApproved() { return approved; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setProduct(Product product) { this.product = product; }
    public void setUser(User user) { this.user = user; }
    public void setRating(int rating) { this.rating = rating; }
    public void setTitle(String title) { this.title = title; }
    public void setComment(String comment) { this.comment = comment; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
