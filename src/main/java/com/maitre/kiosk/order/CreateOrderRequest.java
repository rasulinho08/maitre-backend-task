package com.maitre.kiosk.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull Long productId,
            @NotNull @Min(1) @Max(99) Integer quantity) {
    }
}
