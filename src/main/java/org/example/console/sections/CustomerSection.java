package org.example.console.sections;

import org.example.console.ConsoleIO;
import org.example.console.Fmt;
import org.example.console.Menu;
import org.example.console.MenuSection;
import org.example.console.Table;
import org.example.model.Customer;
import org.example.model.CustomerStatus;
import org.example.model.LoyaltyAccount;
import org.example.model.SourceOperation;
import org.example.model.query.CustomerSort;
import org.example.service.AccountService;
import org.example.service.CustomerService;

import java.util.List;

public class CustomerSection implements MenuSection {

    private final ConsoleIO io;
    private final CustomerService customers;
    private final AccountService accounts;

    public CustomerSection(ConsoleIO io, CustomerService customers, AccountService accounts) {
        this.io = io;
        this.customers = customers;
        this.accounts = accounts;
    }

    @Override
    public String title() {
        return "Клиенты";
    }

    @Override
    public void fill(Menu menu) {
        menu.add("Зарегистрировать клиента", this::register)
                .add("Список клиентов (фильтр и сортировка)", this::list)
                .add("Найти клиентов", this::search)
                .add("Карточка клиента", this::card)
                .add("Открыть дополнительный счёт", this::openAccount)
                .add("Заблокировать / разблокировать клиента", this::toggleStatus)
                .add("Удалить клиента (только без операций)", this::delete);
    }

    private void register() {
        String name = io.text("ФИО");
        String phone = io.text("Телефон");
        String email = io.optionalText("E-mail");
        var birthDate = io.optionalDate("Дата рождения");
        CustomerService.Registration result = customers.register(name, phone, email, birthDate);
        io.success("Клиент #" + result.customer().id() + " зарегистрирован. Номер карты: "
                + result.account().cardNumber());
    }

    private void list() {
        CustomerStatus status = io.chooseOrAny("Статус", List.of(CustomerStatus.values()), Enum::name);
        CustomerSort sort = io.choose("Сортировка", List.of(CustomerSort.values()), CustomerSort::label);
        printCustomers(customers.list(status, sort));
    }

    private void search() {
        String text = io.optionalText("Часть ФИО, телефона или e-mail");
        printCustomers(customers.search(text));
    }

    private void card() {
        Customer customer = customers.getByPhone(io.text("Телефон клиента"));
        io.printf("Клиент #%d: %s%n", customer.id(), customer.fullName());
        io.printf("Телефон: %s   E-mail: %s%n", customer.phone(), Fmt.text(customer.email()));
        io.printf("Дата рождения: %s   Зарегистрирован: %s   Статус: %s%n",
                Fmt.date(customer.birthDate()), Fmt.dateTime(customer.createdAt()), customer.status());

        io.println();
        io.println("Счета:");
        printAccounts(accounts.byCustomer(customer.id()));

        io.println();
        io.println("Операции:");
        Table table = new Table("ID", "Дата", "Тип", "Сумма", "Внешний №", "Магазин");
        for (SourceOperation op : customers.operations(customer.id())) {
            table.row(op.id(), Fmt.dateTime(op.operationDate()), op.type(), Fmt.money(op.amount()),
                    op.externalId(), op.storeCode());
        }
        table.print(io);
    }

    private void openAccount() {
        Customer customer = customers.getByPhone(io.text("Телефон клиента"));
        LoyaltyAccount account = accounts.open(customer.id());
        io.success("Открыт счёт #" + account.id() + ", карта " + account.cardNumber());
    }

    private void toggleStatus() {
        Customer customer = customers.getByPhone(io.text("Телефон клиента"));
        CustomerStatus next = customer.isActive() ? CustomerStatus.BLOCKED : CustomerStatus.ACTIVE;
        if (io.confirm("Клиент сейчас " + customer.status() + ". Перевести в " + next + "?")) {
            customers.setStatus(customer.id(), next);
            io.success("Статус клиента: " + next);
        }
    }

    private void delete() {
        Customer customer = customers.getByPhone(io.text("Телефон клиента"));
        if (io.confirm("Удалить клиента «" + customer.fullName() + "» и его счета безвозвратно?")) {
            customers.delete(customer.id());
            io.success("Клиент #" + customer.id() + " удалён");
        }
    }

    private void printCustomers(List<Customer> list) {
        Table table = new Table("ID", "ФИО", "Телефон", "E-mail", "Статус");
        list.forEach(c -> table.row(c.id(), c.fullName(), c.phone(), c.email(), c.status()));
        table.print(io);
    }

    static void printAccounts(ConsoleIO io, List<LoyaltyAccount> list) {
        Table table = new Table("ID", "Карта", "Баланс", "Уровень", "Статус", "Открыт");
        list.forEach(a -> table.row(a.id(), a.cardNumber(), Fmt.money(a.balance()), a.tier(), a.status(),
                Fmt.dateTime(a.openedAt())));
        table.print(io);
    }

    private void printAccounts(List<LoyaltyAccount> list) {
        printAccounts(io, list);
    }
}
