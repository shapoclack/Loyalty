package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Параметры подключения: db.properties из classpath, переопределяемые переменными окружения. */
public record DbConfig(String url, String user, String password) {

    public static DbConfig load() {
        Properties props = new Properties();
        try (InputStream in = DbConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать db.properties", e);
        }
        String url = pick("LOYALTY_DB_URL", props.getProperty("db.url"), "jdbc:postgresql://localhost:5432/loyalty");
        String user = pick("LOYALTY_DB_USER", props.getProperty("db.user"), System.getProperty("user.name"));
        String password = pick("LOYALTY_DB_PASSWORD", props.getProperty("db.password"), "");
        return new DbConfig(url, user, password);
    }

    private static String pick(String envName, String fileValue, String fallback) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return fileValue != null && !fileValue.isBlank() ? fileValue : fallback;
    }
}
