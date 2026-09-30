package org.example.console.sections;

import org.example.console.ConsoleIO;
import org.example.console.Fmt;
import org.example.console.Menu;
import org.example.console.MenuSection;
import org.example.console.Table;
import org.example.model.AccountStatus;
import org.example.model.AccrualRequest;
import org.example.model.BonusTransaction;
import org.example.model.LoyaltyAccount;
import org.example.service.AccountService;

import java.util.List;

public class AccountSection implements MenuSection {

    private final ConsoleIO io;
    private final AccountService accounts;

    public AccountSection(ConsoleIO io, AccountService accounts) {
        this.io = io;
        this.accounts = accounts;
    }

    @Override
    public String title() {
        return "Бонусные счета";
    }

    @Override
    public void fill(Menu menu) {
        menu.add("Баланс по карте", this::balance)
                .add("История бонусных транзакций", this::transactions)
                .add("Заявки на начисление и списание", this::requests)
                .add("Заблокировать / разблокировать счёт", this::toggleStatus);
    }

    private LoyaltyAccount askAccount() {
        return accounts.getByCard(io.text("Номер карты"));
    }

    private void balance() {
        CustomerSection.printAccounts(io, List.of(askAccount()));
    }

    private void transactions() {
        LoyaltyAccount account = askAccount();
        Table table = new Table("ID", "Дата", "Тип", "Сумма", "Баланс после", "Заявка");
        for (BonusTransaction t : accounts.transactions(account.id())) {
            String request = t.accrualRequestId() != null
                    ? "начисл. #" + t.accrualRequestId()
                    : t.redemptionRequestId() != null ? "списан. #" + t.redemptionRequestId() : null;
            table.row(t.id(), Fmt.dateTime(t.createdAt()), t.type(), Fmt.signed(t.amount()),
                    Fmt.money(t.balanceAfter()), request);
        }
        table.print(io);
        io.println("Текущий баланс: " + Fmt.money(account.balance()));
    }

    private void requests() {
        LoyaltyAccount account = askAccount();
        io.println("Начисления:");
        Table accruals = new Table("ID", "Операция", "Правило", "Бонусы", "Статус", "Создана", "Обработана");
        for (AccrualRequest r : accounts.accrualRequests(account.id())) {
            accruals.row(r.id(), r.operationId(), r.ruleId(), Fmt.money(r.bonusAmount()), r.status(),
                    Fmt.dateTime(r.createdAt()), Fmt.dateTime(r.processedAt()));
        }
        accruals.print(io);
        io.println();
        io.println("Списания:");
        RedemptionSection.printRequests(io, accounts.redemptionRequests(account.id()));
    }

    private void toggleStatus() {
        LoyaltyAccount account = askAccount();
        AccountStatus next = account.isActive() ? AccountStatus.BLOCKED : AccountStatus.ACTIVE;
        if (io.confirm("Счёт сейчас " + account.status() + ". Перевести в " + next + "?")) {
            accounts.setStatus(account.id(), next);
            io.success("Статус счёта: " + next);
        }
    }

}
