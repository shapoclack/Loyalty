package org.example.service;

import org.example.exception.BusinessException;
import org.example.model.*;
import org.example.service.DashboardService.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DashboardTest {
    private final LocalDateTime date = LocalDate.of(2026, 1, 20).atStartOfDay();
    private CustomerRow row(String name, CustomerStatus status, Tier tier, String balance) {
        return new CustomerRow(new Customer(1, name, "+79991234567", "hello@example.ru", null, date, status),
                List.of(new LoyaltyAccount(1, 1, "123", new BigDecimal(balance), tier, AccountStatus.ACTIVE, date, date)));
    }
    @Test void combinesSearchAndBothFilters() {
        var row = row("Иван Петров", CustomerStatus.ACTIVE, Tier.GOLD, "100");
        assertTrue(CustomerViewFilter.of("пЕТРОВ", CustomerStatus.ACTIVE, Tier.GOLD).test(row));
        assertTrue(CustomerViewFilter.of("HELLO@", null, null).test(row));
        assertTrue(CustomerViewFilter.of("+7 (999)", null, null).test(row));
        assertFalse(CustomerViewFilter.of("Иван", CustomerStatus.BLOCKED, Tier.GOLD).test(row));
        assertFalse(CustomerViewFilter.of("Иван", null, Tier.BASE).test(row));
    }
    @Test void calculatesStatisticsIncludingInclusivePeriodAndEmptyData() {
        var snapshot = new Snapshot(List.of(row("А", CustomerStatus.ACTIVE, Tier.GOLD, "100"),
                row("Б", CustomerStatus.BLOCKED, Tier.BASE, "20")),
                List.of(new SourceOperation(1, 1, OperationType.PURCHASE, null, new BigDecimal("1500"), date, null)));
        var service = new StatisticsService();
        var values = service.calculate(snapshot, date.toLocalDate(), date.toLocalDate());
        assertEquals("2", values.get("Всего клиентов"));
        assertEquals("50.0 %", values.get("Доля активных"));
        assertEquals("120.00", values.get("Общий баланс бонусов"));
        assertEquals("60.00", values.get("Средний баланс на клиента"));
        assertEquals("1", values.get("Покупок за период"));
        assertEquals("1500.00", values.get("Средний чек за период"));
        assertEquals("0", service.calculate(snapshot, date.toLocalDate().plusDays(1), null).get("Покупок за период"));
        assertEquals("0.00", service.calculate(new Snapshot(List.of(), List.of()), null, null).get("Средний чек за период"));
        assertThrows(BusinessException.class, () -> service.calculate(snapshot, LocalDate.now(), date.toLocalDate()));
    }
    @Test void exportsOnlySuppliedRowsWithEscapingAndUtf8Bom(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("customers.csv");
        new CustomerCsvExporter().export(file, List.of(row("Иван; \"Петров\"\nТест", CustomerStatus.ACTIVE, Tier.GOLD, "100")));
        String csv = Files.readString(file);
        assertTrue(csv.startsWith("\ufeffФИО;"));
        assertTrue(csv.contains("\"Иван; \"\"Петров\"\"\nТест\""));
        assertTrue(csv.contains("\"GOLD\";\"100\""));
        assertEquals("\"'=SUM(A1)\"", CustomerCsvExporter.cell("=SUM(A1)"));
    }
}
