package com.maitre.kiosk.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING, PREPARING, true",
            "PENDING, CANCELLED, true",
            "PREPARING, READY, true",
            "READY, COMPLETED, true",
            "PENDING, READY, false",
            "PENDING, COMPLETED, false",
            "PENDING, PENDING, false",
            "PREPARING, CANCELLED, false",
            "PREPARING, PENDING, false",
            "READY, CANCELLED, false",
            "READY, PREPARING, false",
            "COMPLETED, CANCELLED, false",
            "COMPLETED, PENDING, false",
            "CANCELLED, PENDING, false",
            "CANCELLED, PREPARING, false"
    })
    void allowsOnlyForwardStepsAndCancelWhilePending(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "Pending", " PENDING "})
    void parsesStatusIgnoringCase(String value) {
        assertThat(OrderStatus.parse(value)).isEqualTo(OrderStatus.PENDING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "DONE", "pend"})
    void rejectsUnknownStatus(String value) {
        assertThatThrownBy(() -> OrderStatus.parse(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
