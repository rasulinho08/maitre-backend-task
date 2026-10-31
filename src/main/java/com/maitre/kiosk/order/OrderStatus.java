package com.maitre.kiosk.order;

import java.util.Locale;

public enum OrderStatus {
    PENDING,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == READY;
            case READY -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public static OrderStatus parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown order status: " + value, ex);
        }
    }
}
