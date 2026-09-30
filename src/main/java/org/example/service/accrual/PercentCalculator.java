package org.example.service.accrual;

import org.example.model.AccrualRule;

import java.math.BigDecimal;

/** value% от суммы операции. */
public class PercentCalculator implements AccrualCalculator {

    @Override
    public String type() {
        return "PERCENT";
    }

    @Override
    public String description() {
        return "процент от суммы покупки (value = %)";
    }

    @Override
    public BigDecimal calculate(AccrualRule rule, BigDecimal baseAmount) {
        return baseAmount.multiply(rule.value()).movePointLeft(2);
    }
}
