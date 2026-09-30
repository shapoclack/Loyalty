package org.example.console.sections;

import org.example.console.ConsoleIO;
import org.example.console.Fmt;
import org.example.console.Menu;
import org.example.console.MenuSection;
import org.example.console.Table;
import org.example.model.LoyaltyAccount;
import org.example.model.RedemptionRequest;
import org.example.model.RequestStatus;
import org.example.model.SourceOperation;
import org.example.service.AccountService;
import org.example.service.CustomerService;
import org.example.service.RedemptionService;

import java.util.List;

/** Ручное списание по уже зарегистрированной операции: заявка -> подтверждение / отмена. */
public class RedemptionSection implements MenuSection {

    private final ConsoleIO io;
    private final RedemptionService redemptions;
    private final AccountService accounts;
    private final CustomerService customers;

    public RedemptionSection(ConsoleIO io, RedemptionService redemptions, AccountService accounts,
                             CustomerService customers) {
        this.io = io;
        this.redemptions = redemptions;
        this.accounts = accounts;
        this.customers = customers;
    }

    @Override
    public String title() {
        return "Списание бонусов";
    }

    @Override
    public void fill(Menu menu) {
        menu.add("Создать заявку на списание", this::create)
                .add("Заявки, ожидающие подтверждения", this::pending)
                .add("Подтвердить заявку", this::confirm)
                .add("Отменить заявку", this::cancel);
    }

    private void create() {
        LoyaltyAccount account = accounts.getByCard(io.text("Номер карты"));
        io.println("Баланс: " + Fmt.money(account.balance()));
        List<SourceOperation> operations = customers.operations(account.customerId());
        Table table = new Table("ID", "Дата", "Сумма", "Внешний №");
        operations.forEach(op -> table.row(op.id(), Fmt.dateTime(op.operationDate()), Fmt.money(op.amount()),
                op.externalId()));
        table.print(io);
        if (operations.isEmpty()) {
            return;
        }
        int operationId = io.integer("ID операции");
        RedemptionRequest request = redemptions.create(account.id(), operationId, io.decimal("Сколько бонусов списать"));
        io.success("Создана заявка #" + request.id() + " на " + Fmt.money(request.bonusAmount()) + " бонусов");
        if (io.confirm("Подтвердить сразу?")) {
            report(redemptions.confirm(request.id()));
        }
    }

    private void pending() {
        printRequests(io, redemptions.pending());
    }

    private void confirm() {
        report(redemptions.confirm(io.integer("ID заявки")));
    }

    private void cancel() {
        RedemptionRequest request = redemptions.cancel(io.integer("ID заявки"));
        io.success("Заявка #" + request.id() + " отменена");
    }

    private void report(RedemptionRequest request) {
        if (request.status() == RequestStatus.APPROVED) {
            io.success("Списано " + Fmt.money(request.bonusAmount()) + " бонусов по заявке #" + request.id());
        } else {
            io.error("Заявка #" + request.id() + " не исполнена, статус: " + request.status());
        }
    }

    static void printRequests(ConsoleIO io, List<RedemptionRequest> list) {
        Table table = new Table("ID", "Счёт", "Операция", "Правило", "Бонусы", "Статус", "Создана", "Обработана");
        list.forEach(r -> table.row(r.id(), r.accountId(), r.operationId(), r.ruleId(), Fmt.money(r.bonusAmount()),
                r.status(), Fmt.dateTime(r.createdAt()), Fmt.dateTime(r.processedAt())));
        table.print(io);
    }
}
