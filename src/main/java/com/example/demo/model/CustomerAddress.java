package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "customer_addresses",
        indexes = {
                @Index(name = "idx_addresses_user", columnList = "user_id"),
                @Index(name = "idx_addresses_city", columnList = "city")
        }
)
public class CustomerAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING)
    private AddressType type = AddressType.SHIPPING;
    @Column(name = "full_name")
    private String fullName;
    private String phone;
    private String country;
    private String city;
    private String street;
    @Column(name = "postal_code")
    private String postalCode;
    @Column(name = "default_address")
    private boolean defaultAddress;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CustomerAddress() {}

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public AddressType getType() { return type; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public String getStreet() { return street; }
    public String getPostalCode() { return postalCode; }
    public boolean isDefaultAddress() { return defaultAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setType(AddressType type) { this.type = type; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setCountry(String country) { this.country = country; }
    public void setCity(String city) { this.city = city; }
    public void setStreet(String street) { this.street = street; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setDefaultAddress(boolean defaultAddress) { this.defaultAddress = defaultAddress; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
