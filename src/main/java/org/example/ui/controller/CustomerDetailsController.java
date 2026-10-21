package org.example.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.example.AppContext;
import org.example.model.*;
import org.example.ui.support.Ui;
import java.util.List;

public final class CustomerDetailsController {
    @FXML private Parent root;
    @FXML private Label title, contact, notice, transactionTitle;
    @FXML private TableView<LoyaltyAccount> accounts;
    @FXML private TableView<SourceOperation> operations;
    @FXML private TableView<BonusTransaction> transactions;
    @FXML private Button blockButton, unblockButton;
    private final AppContext context;
    private final Customer customer;
    private final Runnable changed;
    public CustomerDetailsController(AppContext context, Customer customer, Runnable changed) {
        this.context = context; this.customer = customer; this.changed = changed;
    }
    @FXML private void initialize() {
        title.setText(customer.fullName());
        contact.setText(customer.phone() + "  •  " + Ui.format(customer.email()) + "  •  Дата рождения: " + Ui.format(customer.birthDate()));
        Ui.column(accounts, "Номер карты", LoyaltyAccount::cardNumber, 210);
        Ui.column(accounts, "Баланс бонусов", LoyaltyAccount::balance, 145);
        Ui.column(accounts, "Уровень", LoyaltyAccount::tier, 115);
        Ui.column(accounts, "Статус", LoyaltyAccount::status, 150);
        Ui.column(accounts, "Дата открытия", LoyaltyAccount::openedAt, 180);
        Ui.column(operations, "Дата покупки", SourceOperation::operationDate, 180);
        Ui.column(operations, "Номер чека", SourceOperation::externalId, 200);
        Ui.column(operations, "Магазин", SourceOperation::storeCode, 180);
        Ui.column(operations, "Сумма", SourceOperation::amount, 160);
        Ui.column(transactions, "Дата", BonusTransaction::createdAt, 180);
        Ui.column(transactions, "Тип", BonusTransaction::type, 180);
        Ui.column(transactions, "Бонусы", BonusTransaction::amount, 170);
        Ui.column(transactions, "Баланс после", BonusTransaction::balanceAfter, 190);
        Ui.placeholder(accounts, "Счета отсутствуют"); Ui.placeholder(operations, "Покупок пока нет");
        Ui.placeholder(transactions, "Выберите карту на вкладке «Счета»");
        blockButton.disableProperty().bind(accounts.getSelectionModel().selectedItemProperty().isNull());
        unblockButton.disableProperty().bind(accounts.getSelectionModel().selectedItemProperty().isNull());
        accounts.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> history(value));
    }
    private record Data(List<LoyaltyAccount> accounts, List<SourceOperation> operations) { }
    @FXML public void refresh() {
        notice.setText("Загрузка счетов и операций…");
        Ui.async(root, () -> new Data(context.accounts.byCustomer(customer.id()), context.customers.operations(customer.id())),
                data -> {
                    accounts.setItems(FXCollections.observableArrayList(data.accounts()));
                    operations.setItems(FXCollections.observableArrayList(data.operations()));
                    notice.setText("Счетов: " + data.accounts().size() + ". Покупок: " + data.operations().size());
                    if (!data.accounts().isEmpty()) accounts.getSelectionModel().selectFirst();
                }, error -> { notice.setText(Ui.message(error)); Ui.error(root.getScene().getWindow(), error); });
    }
    private void history(LoyaltyAccount account) {
        transactions.getItems().clear();
        transactionTitle.setText(account == null ? "Выберите карту на вкладке «Счета»" : "История карты " + account.cardNumber());
        if (account == null) return;
        Ui.async(root, () -> context.accounts.transactions(account.id()),
                data -> { transactions.setItems(FXCollections.observableArrayList(data)); Ui.placeholder(transactions, "По этой карте пока нет движений бонусов"); },
                error -> Ui.error(root.getScene().getWindow(), error));
    }
    @FXML private void openAccount() {
        Ui.async(root, () -> context.accounts.open(customer.id()), account -> { refresh(); changed.run(); },
                error -> Ui.error(root.getScene().getWindow(), error));
    }
    @FXML private void block() { setStatus(AccountStatus.BLOCKED); }
    @FXML private void unblock() { setStatus(AccountStatus.ACTIVE); }
    private void setStatus(AccountStatus status) {
        var account = accounts.getSelectionModel().getSelectedItem();
        if (account == null) return;
        Ui.async(root, () -> { context.accounts.setStatus(account.id(), status); return true; },
                ok -> { refresh(); changed.run(); }, error -> Ui.error(root.getScene().getWindow(), error));
    }
}
