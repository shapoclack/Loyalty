package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Map;
import java.nio.file.Path;

/** Подключение: переменные процесса, .env рабочего каталога, затем db.properties и значения по умолчанию. */
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
        return resolve(System.getenv(), DotEnv.read(Path.of(".env")), props);
    }

    static DbConfig resolve(Map<String, String> environment, Map<String, String> dotenv, Properties props) {
        String url = url(environment, dotenv);
        if (url == null) url = url(dotenv, Map.of());
        if (url == null) url = first(props.getProperty("db.url"), "jdbc:postgresql://localhost:5432/loyalty");
        String user = first(environment.get("LOYALTY_DB_USER"), environment.get("POSTGRES_USER"),
                dotenv.get("LOYALTY_DB_USER"), dotenv.get("POSTGRES_USER"),
                props.getProperty("db.user"), System.getProperty("user.name"));
        String password = password(environment);
        if (password == null) password = password(dotenv);
        if (password == null) password = props.getProperty("db.password", "");
        return new DbConfig(url, user, password);
    }

    private static String url(Map<String, String> source, Map<String, String> fallback) {
        String direct = first(source.get("LOYALTY_DB_URL"));
        if (direct != null) return direct;
        if (first(source.get("POSTGRES_HOST"), source.get("POSTGRES_PORT"), source.get("POSTGRES_DB")) == null)
            return null;
        String host = first(source.get("POSTGRES_HOST"), fallback.get("POSTGRES_HOST"), "localhost");
        String port = first(source.get("POSTGRES_PORT"), fallback.get("POSTGRES_PORT"), "5432");
        String database = first(source.get("POSTGRES_DB"), fallback.get("POSTGRES_DB"), "loyalty");
        try {
            int number = Integer.parseInt(port);
            if (number < 1 || number > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalStateException("POSTGRES_PORT: укажите целое число от 1 до 65535");
        }
        if (!host.matches("[a-zA-Z0-9._:\\[\\]-]+") || !database.matches("[a-zA-Z0-9_-]+"))
            throw new IllegalStateException("Проверьте POSTGRES_HOST и POSTGRES_DB; для сложного адреса задайте LOYALTY_DB_URL");
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private static String password(Map<String, String> source) {
        // Пустой пароль тоже является явной настройкой и перекрывает db.properties.
        return source.containsKey("LOYALTY_DB_PASSWORD") ? source.get("LOYALTY_DB_PASSWORD") : source.get("POSTGRES_PASSWORD");
    }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }
}
