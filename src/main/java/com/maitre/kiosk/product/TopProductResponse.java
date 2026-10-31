package com.maitre.kiosk.product;

public record TopProductResponse(Long productId, String name, String category, long quantitySold) {
}
