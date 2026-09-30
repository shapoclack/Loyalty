package org.example;

import org.example.config.DbConfig;
import org.example.console.ConsoleIO;
import org.example.console.LoyaltyConsole;
import org.example.db.DataAccessException;
import org.example.db.Database;

public class Main {

    public static void main(String[] args) {
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
        new LoyaltyConsole(io, new AppContext(db, io).sections()).run();
    }
}
