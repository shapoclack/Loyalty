package org.example.console.sections;

import org.example.console.ConsoleIO;
import org.example.console.Fmt;
import org.example.console.Menu;
import org.example.console.MenuSection;
import org.example.console.Table;
import org.example.model.AccrualRule;
import org.example.model.RedemptionRule;
import org.example.service.RuleService;
import org.example.service.accrual.AccrualCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RuleSection implements MenuSection {

    private final ConsoleIO io;
    private final RuleService rules;

    public RuleSection(ConsoleIO io, RuleService rules) {
        this.io = io;
        this.rules = rules;
    }

    @Override
    public String title() {
        return "Правила начисления и списания";
    }

    @Override
    public void fill(Menu menu) {
        menu.add("Правила начисления", this::listAccrual)
                .add("Добавить правило начисления", this::addAccrual)
                .add("Включить / выключить правило начисления", this::toggleAccrual)
                .add("Удалить правило начисления", this::deleteAccrual)
                .add("Правила списания", this::listRedemption)
                .add("Добавить правило списания", this::addRedemption)
                .add("Включить / выключить правило списания", this::toggleRedemption)
                .add("Удалить правило списания", this::deleteRedemption);
    }

    private void listAccrual() {
        Table table = new Table("ID", "Название", "Тип", "Значение", "Уровень", "С", "По", "Активно");
        for (AccrualRule r : rules.accrualRules()) {
            table.row(r.id(), r.name(), r.ruleType(), Fmt.number(r.value()), r.category() == null ? "все" : r.category(),
                    Fmt.dateTime(r.validFrom()), Fmt.dateTime(r.validTo()), r.active() ? "да" : "нет");
        }
        table.print(io);
        io.println("При покупке применяется самое выгодное из подходящих правил.");
    }

    private void addAccrual() {
        String name = io.text("Название");
        io.println("Тип правила:");
        AccrualCalculator type = io.choose("Номер типа", List.copyOf(rules.accrualTypes()),
                c -> c.type() + " — " + c.description());
        BigDecimal value = io.decimal("Значение (value)");
        String category = io.optionalText("Уровень BASE/SILVER/GOLD (пусто — все)");
        LocalDate from = io.optionalDate("Действует с");
        LocalDate to = io.optionalDate("Действует по (включительно)");
        AccrualRule rule = rules.addAccrualRule(name, type.type(), value, category,
                startOf(from), to == null ? null : to.plusDays(1).atStartOfDay());
        io.success("Добавлено правило начисления #" + rule.id());
    }

    private void toggleAccrual() {
        int id = io.integer("ID правила");
        boolean active = io.confirm("Сделать правило активным? (н — выключить)");
        rules.setAccrualRuleActive(id, active);
        io.success("Правило #" + id + (active ? " включено" : " выключено"));
    }

    private void listRedemption() {
        Table table = new Table("ID", "Название", "Макс. % чека", "Мин. бонусов", "Макс. бонусов", "Срок заявки, дн.",
                "Активно");
        for (RedemptionRule r : rules.redemptionRules()) {
            table.row(r.id(), r.name(), Fmt.number(r.maxPercent()), Fmt.money(r.minAmount()),
                    r.maxAmount() == null ? "без лимита" : Fmt.money(r.maxAmount()), r.expireDays(),
                    r.active() ? "да" : "нет");
        }
        table.print(io);
        io.println("Действует самое новое из активных правил списания.");
    }

    private void addRedemption() {
        String name = io.text("Название");
        BigDecimal maxPercent = io.decimal("Максимальный % чека, оплачиваемый бонусами");
        BigDecimal minAmount = io.optionalDecimal("Минимум бонусов за списание");
        BigDecimal maxAmount = io.optionalDecimal("Максимум бонусов за операцию");
        int expireDays = io.integer("Срок действия заявки, дней");
        RedemptionRule rule = rules.addRedemptionRule(name, maxPercent, minAmount, maxAmount, expireDays);
        io.success("Добавлено правило списания #" + rule.id());
    }

    private void toggleRedemption() {
        int id = io.integer("ID правила");
        boolean active = io.confirm("Сделать правило активным? (н — выключить)");
        rules.setRedemptionRuleActive(id, active);
        io.success("Правило #" + id + (active ? " включено" : " выключено"));
    }

    private void deleteAccrual() {
        int id = io.integer("ID правила");
        if (io.confirm("Удалить правило начисления #" + id + "?")) {
            rules.deleteAccrualRule(id);
            io.success("Правило #" + id + " удалено");
        }
    }

    private void deleteRedemption() {
        int id = io.integer("ID правила");
        if (io.confirm("Удалить правило списания #" + id + "?")) {
            rules.deleteRedemptionRule(id);
            io.success("Правило #" + id + " удалено");
        }
    }

    private static LocalDateTime startOf(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}
