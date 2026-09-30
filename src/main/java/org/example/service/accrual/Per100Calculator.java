package org.example.service.accrual;

import org.example.model.AccrualRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** value бонусов за каждые полные 100 единиц суммы. */
public class Per100Calculator implements AccrualCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public String type() {
        return "PER_100";
    }

    @Override
    public String description() {
        return "бонусы за каждые полные 100 руб. (value = бонусы)";
    }

    @Override
    public BigDecimal calculate(AccrualRule rule, BigDecimal baseAmount) {
        BigDecimal hundreds = baseAmount.divide(HUNDRED, 0, RoundingMode.DOWN);
        return hundreds.multiply(rule.value());
    }
}
