package org.example.service;

import org.example.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StatisticsService {
    public Map<String, String> calculate(DashboardService.Snapshot data, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from))
            throw new BusinessException("Период покупок: дата окончания раньше даты начала");
        var rows = data.customers();
        long active = rows.stream().filter(r -> r.customer().isActive()).count();
        var accounts = rows.stream().flatMap(r -> r.accounts().stream()).toList();
        var balance = rows.stream().map(DashboardService.CustomerRow::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        var purchases = data.operations().stream().filter(o -> (from == null || !o.operationDate().toLocalDate().isBefore(from))
                && (to == null || !o.operationDate().toLocalDate().isAfter(to))).toList();
        var turnover = purchases.stream().map(o -> o.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("Всего клиентов", String.valueOf(rows.size()));
        result.put("Активных клиентов", String.valueOf(active));
        result.put("Заблокированных клиентов", String.valueOf(rows.size() - active));
        result.put("Доля активных", rows.isEmpty() ? "0 %" : BigDecimal.valueOf(active * 100)
                .divide(BigDecimal.valueOf(rows.size()), 1, RoundingMode.HALF_UP) + " %");
        result.put("Бонусных счетов", String.valueOf(accounts.size()));
        result.put("Общий баланс бонусов", money(balance));
        result.put("Средний баланс на клиента", money(average(balance, rows.size())));
        result.put("Минимальный баланс клиента", money(rows.stream().map(DashboardService.CustomerRow::balance).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO)));
        result.put("Максимальный баланс клиента", money(rows.stream().map(DashboardService.CustomerRow::balance).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO)));
        result.put("Покупок за период", String.valueOf(purchases.size()));
        result.put("Сумма покупок за период", money(turnover));
        result.put("Средний чек за период", money(average(turnover, purchases.size())));
        return result;
    }
    private static BigDecimal average(BigDecimal value, long size) {
        return size == 0 ? BigDecimal.ZERO : value.divide(BigDecimal.valueOf(size), 2, RoundingMode.HALF_UP);
    }
    private static String money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
}
