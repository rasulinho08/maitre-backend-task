package com.maitre.kiosk.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

public record UpdateProductRequest(
        @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
        Boolean available) {

    @AssertTrue(message = "at least one of 'price' or 'available' must be provided")
    boolean isAnyFieldPresent() {
        return price != null || available != null;
    }
}
