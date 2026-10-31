package com.maitre.kiosk.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.maitre.kiosk.common.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@PathVariable Long restaurantId,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(restaurantId, request);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse changeStatus(@PathVariable Long restaurantId,
                                      @PathVariable Long orderId,
                                      @Valid @RequestBody ChangeOrderStatusRequest request) {
        return orderService.changeStatus(restaurantId, orderId, request.status());
    }

    @GetMapping
    public PageResponse<OrderResponse> list(@PathVariable Long restaurantId,
                                            @RequestParam(required = false) OrderStatus status,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orderService.list(restaurantId, status, page, size);
    }
}
