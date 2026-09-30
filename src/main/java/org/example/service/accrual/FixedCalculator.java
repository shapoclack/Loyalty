package org.example.service.accrual;

import org.example.model.AccrualRule;

import java.math.BigDecimal;

/** Фиксированное число бонусов за любую операцию. */
public class FixedCalculator implements AccrualCalculator {

    @Override
    public String type() {
        return "FIXED";
    }

    @Override
    public String description() {
        return "фиксированные бонусы за покупку (value = бонусы)";
    }

    @Override
    public BigDecimal calculate(AccrualRule rule, BigDecimal baseAmount) {
        return baseAmount.signum() > 0 ? rule.value() : BigDecimal.ZERO;
    }
}
