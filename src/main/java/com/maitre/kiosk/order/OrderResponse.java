package com.maitre.kiosk.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, Long restaurantId, OrderStatus status, List<Line> lines,
                            BigDecimal total, Instant createdAt, Instant updatedAt) {

    public record Line(Long productId, String productName, int quantity,
                       BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    static OrderResponse from(CustomerOrder order) {
        List<Line> lines = order.getLines().stream()
                .map(line -> new Line(line.getProductId(), line.getProductName(), line.getQuantity(),
                        line.getUnitPrice(), line.getLineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getRestaurantId(), order.getStatus(), lines,
                order.getTotal(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
