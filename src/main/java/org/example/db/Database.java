package org.example.db;

import org.example.config.DbConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.function.Function;

/** Точка доступа к PostgreSQL: открывает соединения и выполняет работу в транзакции. */
public class Database {

    private final DbConfig config;

    public Database(DbConfig config) {
        this.config = config;
    }

    public DbConfig config() {
        return config;
    }

    public <T> T inTransaction(Function<Connection, T> work) {
        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password())) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public void inTransactionVoid(Consumer<Connection> work) {
        inTransaction(connection -> {
            work.accept(connection);
            return null;
        });
    }

    /** Выполняет SQL-скрипт из classpath (несколько операторов за один вызов). */
    public void runScript(String resource) {
        String sql;
        try (InputStream in = Database.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Скрипт не найден: " + resource);
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Не удалось прочитать " + resource, e);
        }
        inTransactionVoid(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        });
    }
}
