package org.example.service;

import org.example.exception.BusinessException;
import java.math.BigDecimal;

public final class PurchaseValidation {
    private PurchaseValidation() { }
    public static BigDecimal number(String text, String field) {
        try {
            BigDecimal value = new BigDecimal(text.strip().replace(',', '.'));
            money(value, field, false);
            return value;
        } catch (NumberFormatException | NullPointerException e) {
            throw new BusinessException(field + ": введите число, например 1250,50");
        }
    }
    public static void validate(BigDecimal amount, BigDecimal bonus, String externalId, String store) {
        money(amount, "Сумма покупки", true);
        if (bonus != null) {
            money(bonus, "Бонусы к списанию", false);
            if (bonus.compareTo(amount) > 0) throw new BusinessException("Бонусы к списанию не могут превышать сумму покупки");
        }
        if (externalId != null && externalId.length() > 64) throw new BusinessException("Номер чека: максимум 64 символа");
        if (store != null && store.length() > 32) throw new BusinessException("Код магазина: максимум 32 символа");
    }
    private static void money(BigDecimal value, String field, boolean positive) {
        if (value == null || value.signum() < 0 || (positive && value.signum() == 0)
                || value.compareTo(new BigDecimal("999999999999.99")) > 0 || value.scale() > 2)
            throw new BusinessException(field + ": " + (positive ? "от 0,01" : "от 0") + " до 999999999999,99; максимум 2 знака после запятой");
    }
}
