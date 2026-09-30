package org.example.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Бонусы хранятся с точностью до копейки и округляются вниз. */
public final class Money {

    private Money() {
    }

    public static BigDecimal bonus(BigDecimal value) {
        return value.setScale(2, RoundingMode.DOWN);
    }

    public static BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO.setScale(2) : value;
    }
}
