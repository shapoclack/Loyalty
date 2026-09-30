package org.example.service;

import org.example.db.Database;
import org.example.exception.BusinessException;
import org.example.exception.EntityNotFoundException;
import org.example.model.AccrualRule;
import org.example.model.RedemptionRule;
import org.example.model.Tier;
import org.example.repository.AccrualRuleRepository;
import org.example.repository.RedemptionRuleRepository;
import org.example.service.accrual.AccrualCalculator;
import org.example.service.accrual.AccrualCalculatorRegistry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class RuleService {

    private final Database db;
    private final AccrualRuleRepository accrualRules;
    private final RedemptionRuleRepository redemptionRules;
    private final AccrualCalculatorRegistry calculators;

    public RuleService(Database db, AccrualRuleRepository accrualRules, RedemptionRuleRepository redemptionRules,
                       AccrualCalculatorRegistry calculators) {
        this.db = db;
        this.accrualRules = accrualRules;
        this.redemptionRules = redemptionRules;
        this.calculators = calculators;
    }

    public Collection<AccrualCalculator> accrualTypes() {
        return calculators.all();
    }

    public List<AccrualRule> accrualRules() {
        return db.inTransaction(accrualRules::findAll);
    }

    public AccrualRule addAccrualRule(String name, String ruleType, BigDecimal value, String category,
                                      LocalDateTime validFrom, LocalDateTime validTo) {
        requireName(name);
        AccrualCalculator calculator = calculators.find(ruleType)
                .orElseThrow(() -> new BusinessException("Неизвестный тип правила: " + ruleType));
        if (value == null || value.signum() < 0) {
            throw new BusinessException("Значение правила не может быть отрицательным");
        }
        String normalizedCategory = normalizeCategory(category);
        LocalDateTime from = validFrom == null ? LocalDateTime.now() : validFrom;
        if (validTo != null && !validTo.isAfter(from)) {
            throw new BusinessException("Дата окончания должна быть позже даты начала");
        }
        return db.inTransaction(c -> accrualRules.insert(c, name.trim(), calculator.type(), value,
                normalizedCategory, from, validTo));
    }

    public void setAccrualRuleActive(int ruleId, boolean active) {
        int updated = db.inTransaction(c -> accrualRules.setActive(c, ruleId, active));
        if (updated == 0) {
            throw new EntityNotFoundException("Правило начисления #" + ruleId + " не найдено");
        }
    }

    /** Удаляет правило, которое ещё ни разу не применялось; использованное правило можно только выключить. */
    public void deleteAccrualRule(int ruleId) {
        db.inTransactionVoid(c -> {
            if (accrualRules.isUsed(c, ruleId)) {
                throw new BusinessException("Правило #" + ruleId + " уже применялось — удалить нельзя, выключите его");
            }
            if (accrualRules.delete(c, ruleId) == 0) {
                throw new EntityNotFoundException("Правило начисления #" + ruleId + " не найдено");
            }
        });
    }

    public List<RedemptionRule> redemptionRules() {
        return db.inTransaction(redemptionRules::findAll);
    }

    public RedemptionRule addRedemptionRule(String name, BigDecimal maxPercent, BigDecimal minAmount,
                                            BigDecimal maxAmount, int expireDays) {
        requireName(name);
        if (maxPercent == null || maxPercent.signum() <= 0 || maxPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Процент должен быть в диапазоне (0; 100]");
        }
        BigDecimal min = minAmount == null ? BigDecimal.ZERO : minAmount;
        if (min.signum() < 0) {
            throw new BusinessException("Минимальная сумма не может быть отрицательной");
        }
        if (maxAmount != null && maxAmount.compareTo(min) < 0) {
            throw new BusinessException("Максимальная сумма меньше минимальной");
        }
        if (expireDays <= 0) {
            throw new BusinessException("Срок действия заявки должен быть больше нуля");
        }
        return db.inTransaction(c -> redemptionRules.insert(c, name.trim(), maxPercent, min, maxAmount, expireDays));
    }

    public void setRedemptionRuleActive(int ruleId, boolean active) {
        int updated = db.inTransaction(c -> redemptionRules.setActive(c, ruleId, active));
        if (updated == 0) {
            throw new EntityNotFoundException("Правило списания #" + ruleId + " не найдено");
        }
    }

    public void deleteRedemptionRule(int ruleId) {
        db.inTransactionVoid(c -> {
            if (redemptionRules.isUsed(c, ruleId)) {
                throw new BusinessException("Правило #" + ruleId + " уже применялось — удалить нельзя, выключите его");
            }
            if (redemptionRules.delete(c, ruleId) == 0) {
                throw new EntityNotFoundException("Правило списания #" + ruleId + " не найдено");
            }
        });
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String upper = category.trim().toUpperCase(Locale.ROOT);
        if (Arrays.stream(Tier.values()).noneMatch(t -> t.name().equals(upper))) {
            throw new BusinessException("Категория должна быть одним из уровней: " + Arrays.toString(Tier.values()));
        }
        return upper;
    }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Название обязательно");
        }
    }
}
