package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Правило начисления. {@code ruleType} выбирает алгоритм расчёта (AccrualCalculator),
 * {@code category} ограничивает правило уровнем счёта (null — для всех уровней).
 */
public record AccrualRule(
        int id,
        String name,
        String ruleType,
        BigDecimal value,
        String category,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        boolean active) {

    public boolean isApplicable(LocalDateTime at, Tier tier) {
        return active
                && !at.isBefore(validFrom)
                && (validTo == null || at.isBefore(validTo))
                && (category == null || category.equalsIgnoreCase(tier.name()));
    }
}
