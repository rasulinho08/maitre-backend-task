package com.maitre.kiosk.order;

import java.math.BigDecimal;

import com.maitre.kiosk.common.MoneyConverter;
import com.maitre.kiosk.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Name and unit price are copied from the product when the order is placed, so later
 * product changes do not rewrite order history.
 */
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private CustomerOrder order;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, updatable = false)
    private String productName;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "unit_price_cents", nullable = false, updatable = false)
    private BigDecimal unitPrice;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "line_total_cents", nullable = false, updatable = false)
    private BigDecimal lineTotal;

    protected OrderLine() {
    }

    OrderLine(CustomerOrder order, Product product, int quantity) {
        this.order = order;
        this.productId = product.getId();
        this.productName = product.getName();
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
