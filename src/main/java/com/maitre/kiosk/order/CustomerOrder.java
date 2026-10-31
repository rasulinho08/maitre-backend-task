package com.maitre.kiosk.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.maitre.kiosk.common.ConflictException;
import com.maitre.kiosk.common.InstantConverter;
import com.maitre.kiosk.common.MoneyConverter;
import com.maitre.kiosk.product.Product;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false, updatable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "total_cents", nullable = false)
    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    @Convert(converter = InstantConverter.class)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Convert(converter = InstantConverter.class)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected CustomerOrder() {
    }

    public CustomerOrder(Long restaurantId, Instant now) {
        this.restaurantId = restaurantId;
        this.status = OrderStatus.PENDING;
        this.total = BigDecimal.ZERO.setScale(2);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addLine(Product product, int quantity) {
        OrderLine line = new OrderLine(this, product, quantity);
        lines.add(line);
        total = total.add(line.getLineTotal());
    }

    public void changeStatus(OrderStatus next, Instant now) {
        if (!status.canTransitionTo(next)) {
            throw new ConflictException("Order " + id + " cannot move from " + status + " to " + next + ".");
        }
        this.status = next;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
