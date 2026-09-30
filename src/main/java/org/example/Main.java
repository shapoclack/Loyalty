package org.example;

import org.example.config.DbConfig;
import org.example.console.ConsoleIO;
import org.example.console.LoyaltyConsole;
import org.example.db.Database;

import java.nio.file.Path;
import org.example.exception.DataAccessException;

public class Main {

    /** Каталог выгрузок Excel; переопределяется переменной окружения LOYALTY_EXPORT_DIR. */
    private static final String DEFAULT_EXPORT_DIR = "exports";

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        // Apache POI пишет логи через Log4j API; без этого он жалуется на отсутствие реализации логгера.
        if (System.getProperty("log4j.provider") == null) {
            System.setProperty("log4j.provider", "org.apache.logging.log4j.simple.internal.SimpleProvider");
        }

        DbConfig config = DbConfig.load();
        Database db = new Database(config);
        try {
            db.runScript("/db/schema.sql");
            db.runScript("/db/seed.sql");
        } catch (DataAccessException e) {
            System.err.println("Не удалось подготовить базу данных " + config.url() + " (пользователь "
                    + config.user() + "): " + e.getMessage());
            System.err.println("Проверьте, что PostgreSQL запущен и база создана: createdb loyalty");
            System.exit(1);
        }

        ConsoleIO io = new ConsoleIO();
        Path exportDir = Path.of(System.getenv().getOrDefault("LOYALTY_EXPORT_DIR", DEFAULT_EXPORT_DIR));
        new LoyaltyConsole(io, new AppContext(db, exportDir, io).sections()).run();
    }
}
