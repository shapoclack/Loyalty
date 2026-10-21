package org.example.service;

import org.example.AppContext;
import org.example.config.DbConfig;
import org.example.db.Database;
import org.example.exception.BusinessException;
import org.example.model.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Проверяется в отдельной временной схеме: существующие данные не изменяются. */
@EnabledIfSystemProperty(named = "loyalty.integration", matches = "true")
class PostgresIntegrationTest {
    @Test void customerCrudAndPurchaseWithRealPostgres() throws Exception {
        DbConfig base = DbConfig.load();
        String schema = "loyalty_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = DriverManager.getConnection(base.url(), base.user(), base.password()); var statement = c.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                String separator = base.url().contains("?") ? "&" : "?";
                Database db = new Database(new DbConfig(base.url() + separator + "currentSchema=" + schema, base.user(), base.password()));
                var dashboard = new DashboardService(db);
                assertEquals(6, dashboard.load().customers().size());
                var app = new AppContext(db);
                var registered = app.customers.register("Тестовый клиент", "+79990000001", "test@example.ru", null);
                assertEquals(1, app.accounts.byCustomer(registered.customer().id()).size());
                app.customers.update(registered.customer().id(), "Новое имя", "+79990000002", null, null, CustomerStatus.BLOCKED);
                assertEquals("Новое имя", app.customers.get(registered.customer().id()).fullName());
                assertThrows(BusinessException.class, () -> app.customers.register("Дубликат", "+79990000002", null, null));
                app.customers.delete(registered.customer().id());
                assertTrue(app.accounts.byCustomer(registered.customer().id()).isEmpty());
                var purchaser = app.customers.register("Покупатель", "+79990000003", null, null);
                var result = app.purchases.purchase(purchaser.account().cardNumber(), new BigDecimal("1000.00"), "TEST-1", "TEST", BigDecimal.ZERO);
                assertEquals(new BigDecimal("1000.00"), result.operation().amount());
                assertEquals(1, app.customers.operations(purchaser.customer().id()).size());
                assertThrows(BusinessException.class, () -> app.customers.delete(purchaser.customer().id()));
                assertThrows(BusinessException.class, () -> app.purchases.purchase(purchaser.account().cardNumber(), BigDecimal.TEN, "TEST-1", null, BigDecimal.ZERO));
                assertEquals(1, app.customers.operations(purchaser.customer().id()).size());
                assertEquals(7, dashboard.load().customers().size());
            } finally {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
