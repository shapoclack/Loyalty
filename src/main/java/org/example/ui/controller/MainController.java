package org.example.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.AppContext;
import org.example.model.*;
import org.example.service.*;
import org.example.service.DashboardService.CustomerRow;
import org.example.ui.support.Ui;
import java.util.List;

public final class MainController {
    @FXML private Parent root;
    @FXML private TextField search;
    @FXML private ComboBox<String> statusFilter, tierFilter;
    @FXML private TableView<CustomerRow> table;
    @FXML private Button editButton, detailsButton, deleteButton, purchaseButton;
    @FXML private Label count, notice;
    private final AppContext context;
    private final DashboardService dashboard;
    private final ObservableList<CustomerRow> rows = FXCollections.observableArrayList();
    private final FilteredList<CustomerRow> filtered = new FilteredList<>(rows);
    private DashboardService.Snapshot snapshot;
    public MainController(AppContext context, DashboardService dashboard) {
        this.context = context; this.dashboard = dashboard;
    }
    @FXML private void initialize() {
        Ui.column(table, "ФИО", r -> r.customer().fullName(), 240);
        Ui.column(table, "Телефон", r -> r.customer().phone(), 160);
        Ui.column(table, "Email", r -> r.customer().email(), 200);
        Ui.column(table, "Статус", r -> Ui.status(r.customer().status()), 130);
        Ui.column(table, "Уровни карт", CustomerRow::tiers, 130);
        Ui.column(table, "Баланс бонусов", CustomerRow::balance, 140);
        Ui.column(table, "Регистрация", r -> r.customer().createdAt(), 165);
        Ui.placeholder(table, "Клиенты не найдены. Измените фильтры или добавьте клиента.");
        SortedList<CustomerRow> sorted = new SortedList<>(filtered);
        // TableView задаёт компаратор при нажатии на заголовок, SortedList меняет порядок отображения.
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        statusFilter.getItems().setAll("Все статусы", "Активен", "Заблокирован");
        tierFilter.getItems().setAll("Все уровни", "BASE", "SILVER", "GOLD");
        statusFilter.getSelectionModel().selectFirst(); tierFilter.getSelectionModel().selectFirst();
        search.textProperty().addListener((o, old, value) -> filter());
        statusFilter.valueProperty().addListener((o, old, value) -> filter());
        tierFilter.valueProperty().addListener((o, old, value) -> filter());
        var none = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(none); detailsButton.disableProperty().bind(none);
        deleteButton.disableProperty().bind(none); purchaseButton.disableProperty().bind(none);
        table.setRowFactory(t -> {
            TableRow<CustomerRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) details(); });
            return row;
        });
    }
    private Window owner() { return root.getScene().getWindow(); }
    @FXML public void refresh() {
        Integer selectedId = selected() == null ? null : selected().customer().id();
        notice.setText("Загрузка данных…");
        Ui.async(root, dashboard::load, data -> {
            snapshot = data; rows.setAll(data.customers()); filter();
            if (selectedId != null) table.getItems().stream().filter(r -> r.customer().id() == selectedId).findFirst()
                    .ifPresent(table.getSelectionModel()::select);
            notice.setText("Данные обновлены. Доступно клиентов: " + rows.size());
        }, error -> { notice.setText(Ui.message(error)); Ui.error(owner(), error); });
    }
    private CustomerRow selected() { return table.getSelectionModel().getSelectedItem(); }
    private void filter() {
        int status = statusFilter.getSelectionModel().getSelectedIndex();
        int tier = tierFilter.getSelectionModel().getSelectedIndex();
        filtered.setPredicate(CustomerViewFilter.of(search.getText(),
                status <= 0 ? null : CustomerStatus.values()[status - 1],
                tier <= 0 ? null : Tier.values()[tier - 1]));
        count.setText("Показано " + filtered.size() + " из " + rows.size());
    }
    @FXML private void reset() {
        search.clear(); statusFilter.getSelectionModel().selectFirst(); tierFilter.getSelectionModel().selectFirst();
    }
    @FXML private void add() { form(null); }
    @FXML private void edit() { if (selected() != null) form(selected().customer()); }
    private void form(Customer customer) {
        var controller = new CustomerFormController(context.customers, customer, () -> { if (customer == null) reset(); refresh(); });
        Ui.window(owner(), customer == null ? "Регистрация клиента" : "Редактирование клиента",
                Ui.load("customer-form.fxml", controller)).showAndWait();
    }
    @FXML private void delete() {
        var row = selected();
        if (row == null || !Ui.confirm(owner(), row.customer().fullName()
                + "\nБудут удалены клиент и его счета. Клиента с историей операций удалить нельзя.")) return;
        Ui.async(root, () -> { context.customers.delete(row.customer().id()); return true; }, ok -> refresh(), error -> Ui.error(owner(), error));
    }
    @FXML private void details() {
        if (selected() == null) return;
        var controller = new CustomerDetailsController(context, selected().customer(), this::refresh);
        var stage = Ui.window(owner(), "Счета и операции клиента", Ui.load("customer-details.fxml", controller));
        stage.setMinWidth(820); stage.setMinHeight(570);
        stage.show(); controller.refresh();
    }
    @FXML private void purchase() {
        if (selected() == null) return;
        var row = selected();
        var available = row.accounts().stream().filter(LoyaltyAccount::isActive).toList();
        if (!row.customer().isActive() || available.isEmpty()) {
            Ui.info(owner(), "Для покупки нужен активный клиент с активной картой. Откройте или разблокируйте счёт в карточке клиента."); return;
        }
        var controller = new PurchaseController(context.purchases, row.customer(), available, this::refresh);
        Ui.window(owner(), "Регистрация покупки", Ui.load("purchase.fxml", controller)).showAndWait();
    }
    @FXML private void statistics() {
        if (snapshot == null) { Ui.info(owner(), "Сначала загрузите данные кнопкой «Обновить»."); return; }
        Ui.window(owner(), "Статистика программы лояльности",
                Ui.load("statistics.fxml", new StatisticsController(snapshot))).show();
    }
    @FXML private void export() {
        if (snapshot == null) { Ui.info(owner(), "Сначала загрузите данные кнопкой «Обновить»."); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт текущего списка клиентов"); chooser.setInitialFileName("loyalty-customers.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        var file = chooser.showSaveDialog(owner());
        if (file == null) return;
        var current = List.copyOf(table.getItems());
        Ui.async(root, () -> { new CustomerCsvExporter().export(file.toPath(), current); return current.size(); },
                size -> Ui.info(owner(), "Экспортировано записей: " + size + "\n" + file), error -> Ui.error(owner(), error));
    }
}
