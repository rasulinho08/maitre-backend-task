package com.maitre.kiosk.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.maitre.kiosk.common.InstantConverter;
import com.maitre.kiosk.common.MoneyConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false, updatable = false)
    private Long restaurantId;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private String category;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "price_cents", nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available;

    @Convert(converter = InstantConverter.class)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Convert(converter = InstantConverter.class)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(Long restaurantId, String name, String category, BigDecimal price, Instant now) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.category = category;
        this.price = normalize(price);
        this.available = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changePrice(BigDecimal newPrice, Instant now) {
        this.price = normalize(newPrice);
        this.updatedAt = now;
    }

    public void changeAvailability(boolean available, Instant now) {
        this.available = available;
        this.updatedAt = now;
    }

    private static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }
}
