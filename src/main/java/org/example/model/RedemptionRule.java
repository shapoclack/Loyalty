package org.example.model;

import java.math.BigDecimal;

/**
 * Правило списания: не более {@code maxPercent}% суммы операции,
 * в пределах [{@code minAmount}; {@code maxAmount}] бонусов за операцию.
 * Заявка на списание действительна {@code expireDays} дней.
 */
public record RedemptionRule(
        int id,
        String name,
        BigDecimal maxPercent,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        int expireDays,
        boolean active) {
}
