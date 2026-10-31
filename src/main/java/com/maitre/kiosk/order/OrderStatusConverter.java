package com.maitre.kiosk.order;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Lets clients use any casing for the status query parameter (?status=pending).
 */
@Component
public class OrderStatusConverter implements Converter<String, OrderStatus> {

    @Override
    public OrderStatus convert(String source) {
        return OrderStatus.parse(source);
    }
}
