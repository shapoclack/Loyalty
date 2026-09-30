package org.example.service.accrual;

import org.example.model.AccrualRule;

import java.math.BigDecimal;

/**
 * Алгоритм расчёта бонусов для одного типа правила (accrual_rule.rule_type).
 * Чтобы добавить новый тип правила, реализуйте интерфейс и зарегистрируйте
 * реализацию в {@link AccrualCalculatorRegistry#defaults()}.
 */
public interface AccrualCalculator {

    /** Значение accrual_rule.rule_type, которое обрабатывает калькулятор. */
    String type();

    /** Пояснение смысла поля value для пользователя. */
    String description();

    /** Бонусы за операцию на сумму {@code baseAmount} (без округления). */
    BigDecimal calculate(AccrualRule rule, BigDecimal baseAmount);
}
