package com.maitre.kiosk.product;

import java.math.BigDecimal;

public record ProductResponse(Long id, Long restaurantId, String name, String category,
                              BigDecimal price, boolean available) {

    static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getRestaurantId(), product.getName(),
                product.getCategory(), product.getPrice(), product.isAvailable());
    }
}
