package org.example.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.AppContext;
import org.example.config.DbConfig;
import org.example.db.Database;
import org.example.model.*;
import org.example.service.DashboardService;
import org.example.ui.controller.*;
import org.example.ui.support.Ui;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Требуется оконная сессия. Загружает FXML, применяет CSS и проверяет обработчики формы. */
@EnabledIfSystemProperty(named = "loyalty.uiTest", matches = "true")
class FxmlSmokeTest {
    @Test void loadsAllWindowsAndValidatesCustomerForm() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        assertTrue(ready.await(15, TimeUnit.SECONDS));
        FutureTask<Void> check = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            Database db = new Database(DbConfig.load());
            var app = new AppContext(db);
            var now = LocalDateTime.now();
            var customer = new Customer(1, "Иван Петров", "+79990000001", "mail@example.ru", null, now, CustomerStatus.ACTIVE);
            var account = new LoyaltyAccount(1, 1, "7700000000000001", BigDecimal.TEN, Tier.BASE, AccountStatus.ACTIVE, now, now);
            Object[][] windows = {
                {"main.fxml", new MainController(app, new DashboardService(db))},
                {"customer-form.fxml", new CustomerFormController(app.customers, null, () -> {})},
                {"customer-form.fxml", new CustomerFormController(app.customers, customer, () -> {})},
                {"customer-details.fxml", new CustomerDetailsController(app, customer, () -> {})},
                {"purchase.fxml", new PurchaseController(app.purchases, customer, List.of(account), () -> {})},
                {"statistics.fxml", new StatisticsController(new DashboardService.Snapshot(List.of(), List.of()))}
            };
            for (Object[] window : windows) {
                var root = Ui.load((String) window[0], window[1]);
                var scene = new Scene(root);
                scene.getStylesheets().add(Ui.class.getResource("/org/example/ui/style.css").toExternalForm());
                Stage stage = new Stage(); stage.setScene(scene); stage.show(); root.applyCss(); root.layout();
                assertTrue(root.getLayoutBounds().getWidth() > 0);
                if (window[1] instanceof CustomerFormController) {
                    TextField name = (TextField) root.lookup("#name");
                    String initial = name.getText();
                    name.replaceText(0, name.getLength(), "Анна-Мария Ёлкина");
                    assertEquals("Анна-Мария Ёлкина", name.getText());
                    name.appendText("123");
                    assertEquals("Анна-Мария Ёлкина", name.getText());
                    name.replaceText(0, name.getLength(), "12345");
                    assertEquals("Анна-Мария Ёлкина", name.getText());
                    assertTrue(((Label) root.lookup("#error")).getText().contains("ФИО:"));
                    name.replaceText(0, name.getLength(), initial);
                }
                if (window[1] instanceof CustomerFormController && ((TextField) root.lookup("#name")).getText().isEmpty()) {
                    root.lookupAll(".button").stream().filter(n -> ((Button) n).getText().equals("Сохранить"))
                            .forEach(n -> ((Button) n).fire());
                    assertTrue(((Label) root.lookup("#error")).getText().contains("ФИО"));
                    ((TextField) root.lookup("#name")).setText("Иван Петров");
                    ((TextField) root.lookup("#phone")).setText("+79990000001");
                    DatePicker birthDate = (DatePicker) root.lookup("#birthDate");
                    birthDate.getEditor().setText("31.02.2026");
                    root.lookupAll(".button").stream().filter(n -> ((Button) n).getText().equals("Сохранить"))
                            .forEach(n -> ((Button) n).fire());
                    assertTrue(((Label) root.lookup("#error")).getText().contains("существующую дату"));
                }
                stage.close();
            }
            return null;
        });
        Platform.runLater(check);
        try { check.get(30, TimeUnit.SECONDS); } finally { Platform.exit(); }
    }
}
