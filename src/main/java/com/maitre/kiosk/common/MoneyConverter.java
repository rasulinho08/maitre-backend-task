package com.maitre.kiosk.common;

import java.math.BigDecimal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Money is stored as integer cents. SQLite has no exact decimal type, so storing
 * BigDecimal directly would end up as a floating point value.
 */
@Converter
public class MoneyConverter implements AttributeConverter<BigDecimal, Long> {

    @Override
    public Long convertToDatabaseColumn(BigDecimal amount) {
        return amount == null ? null : amount.movePointRight(2).longValueExact();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long cents) {
        return cents == null ? null : BigDecimal.valueOf(cents, 2);
    }
}
