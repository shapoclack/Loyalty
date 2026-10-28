package org.example.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.Customer;
import org.example.model.CustomerStatus;
import org.example.service.CustomerService;
import org.example.service.CustomerValidation;
import org.example.ui.support.Ui;
import javafx.util.StringConverter;

public final class CustomerFormController {
    @FXML private Parent root;
    @FXML private TextField name, phone, email;
    @FXML private DatePicker birthDate;
    @FXML private ComboBox<CustomerStatus> status;
    @FXML private Label error, title;
    private final CustomerService service;
    private final Customer customer;
    private final Runnable saved;
    public CustomerFormController(CustomerService service, Customer customer, Runnable saved) {
        this.service = service; this.customer = customer; this.saved = saved;
    }
    @FXML private void initialize() {
        Ui.dates(birthDate);
        status.getItems().setAll(CustomerStatus.values());
        status.setConverter(new StringConverter<>() {
            @Override public String toString(CustomerStatus value) { return value == null ? "" : Ui.status(value); }
            @Override public CustomerStatus fromString(String value) { throw new UnsupportedOperationException(); }
        });
        status.setValue(customer == null ? CustomerStatus.ACTIVE : customer.status());
        status.setDisable(customer == null);
        title.setText(customer == null ? "Новый клиент" : "Редактирование клиента");
        if (customer != null) {
            name.setText(customer.fullName()); phone.setText(customer.phone());
            email.setText(customer.email()); birthDate.setValue(customer.birthDate());
        }
        name.setTextFormatter(new TextFormatter<String>(change -> {
            if (!change.isContentChange()) return change;
            if (!CustomerValidation.isNameInput(change.getControlNewText())) {
                error.setText(CustomerValidation.NAME_FORMAT_MESSAGE);
                return null;
            }
            if (CustomerValidation.NAME_FORMAT_MESSAGE.equals(error.getText())) error.setText("");
            return change;
        }));
    }
    @FXML private void save() {
        try {
            var date = Ui.date(birthDate, "Дата рождения");
            var fullName = name.getText(); var phoneValue = phone.getText(); var emailValue = email.getText();
            var selectedStatus = status.getValue();
            CustomerValidation.validate(fullName, phoneValue, emailValue, date);
            error.setText("Сохранение…");
            Ui.async(root, () -> {
                if (customer == null) service.register(fullName, phoneValue, emailValue, date);
                else service.update(customer.id(), fullName, phoneValue, emailValue, date, selectedStatus);
                return true;
            }, result -> { close(); saved.run(); }, failure -> error.setText(Ui.message(failure)));
        } catch (RuntimeException e) { error.setText(Ui.message(e)); }
    }
    @FXML private void close() { ((Stage) root.getScene().getWindow()).close(); }
}
