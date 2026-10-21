package org.example.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.model.Customer;
import org.example.model.LoyaltyAccount;
import org.example.service.PurchaseService;
import org.example.service.PurchaseValidation;
import org.example.ui.support.Ui;
import org.example.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;

public final class PurchaseController {
    @FXML private Parent root;
    @FXML private Label customerName, error, limit;
    @FXML private ComboBox<LoyaltyAccount> card;
    @FXML private TextField amount, bonus, receipt, store;
    private final PurchaseService service;
    private final Customer customer;
    private final List<LoyaltyAccount> accounts;
    private final Runnable saved;
    public PurchaseController(PurchaseService service, Customer customer, List<LoyaltyAccount> accounts, Runnable saved) {
        this.service = service; this.customer = customer; this.accounts = accounts; this.saved = saved;
    }
    @FXML private void initialize() {
        customerName.setText(customer.fullName()); card.getItems().setAll(accounts);
        card.setConverter(new StringConverter<>() {
            @Override public String toString(LoyaltyAccount a) {
                return a == null ? "" : a.cardNumber() + " • " + a.tier() + " • " + Ui.format(a.balance()) + " бонусов";
            }
            @Override public LoyaltyAccount fromString(String value) { throw new UnsupportedOperationException(); }
        });
        card.getSelectionModel().selectFirst(); bonus.setText("0");
        card.valueProperty().addListener((o, old, value) -> limit.setText(""));
        amount.textProperty().addListener((o, old, value) -> limit.setText(""));
    }
    @FXML private void limit() {
        try {
            var selected = selectedCard(); var sum = PurchaseValidation.number(amount.getText(), "Сумма покупки");
            PurchaseValidation.validate(sum, BigDecimal.ZERO, null, null);
            Ui.async(root, () -> service.redeemable(selected.cardNumber(), sum),
                    value -> { limit.setText("Можно списать до " + Ui.format(value) + " бонусов (с учётом правила и баланса)"); error.setText(""); },
                    failure -> error.setText(Ui.message(failure)));
        } catch (RuntimeException e) { error.setText(Ui.message(e)); }
    }
    @FXML private void save() {
        try {
            var selected = selectedCard(); var sum = PurchaseValidation.number(amount.getText(), "Сумма покупки");
            var redeem = PurchaseValidation.number(bonus.getText(), "Бонусы к списанию");
            var externalId = optional(receipt.getText()); var storeCode = optional(store.getText());
            PurchaseValidation.validate(sum, redeem, externalId, storeCode);
            error.setText("Регистрация покупки…");
            Ui.async(root, () -> service.purchase(selected.cardNumber(), sum, externalId, storeCode, redeem),
                    result -> {
                        var owner = ((Stage) root.getScene().getWindow()).getOwner();
                        close(); saved.run();
                        Ui.info(owner, "Покупка зарегистрирована.\nОплачено деньгами: " + Ui.format(result.paidByMoney())
                                + "\nНачислено бонусов: " + Ui.format(result.accrual().bonusAmount())
                                + "\nНовый баланс: " + Ui.format(result.account().balance()));
                    }, failure -> error.setText(Ui.message(failure)));
        } catch (RuntimeException e) { error.setText(Ui.message(e)); }
    }
    private LoyaltyAccount selectedCard() {
        if (card.getValue() == null) throw new BusinessException("Выберите карту клиента");
        return card.getValue();
    }
    private static String optional(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    @FXML private void close() { ((Stage) root.getScene().getWindow()).close(); }
}
