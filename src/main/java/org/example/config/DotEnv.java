package org.example.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Читает KEY=VALUE в UTF-8. Значения не исполняются и не подставляются как shell-код. */
final class DotEnv {
    private DotEnv() { }

    static Map<String, String> read(Path file) {
        if (Files.notExists(file)) return Map.of();
        try {
            Map<String, String> values = new LinkedHashMap<>();
            int lineNumber = 0;
            for (String source : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                lineNumber++;
                String line = source.strip();
                if (lineNumber == 1 && line.startsWith("\ufeff")) line = line.substring(1).strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int separator = line.indexOf('=');
                if (separator < 1 || !line.substring(0, separator).strip().matches("[A-Za-z_][A-Za-z0-9_]*"))
                    throw invalid(lineNumber);
                String key = line.substring(0, separator).strip();
                String value = line.substring(separator + 1).strip();
                if (value.startsWith("\"") || value.startsWith("'")) {
                    if (value.length() < 2 || value.charAt(value.length() - 1) != value.charAt(0))
                        throw invalid(lineNumber);
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
            return values;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать .env в рабочем каталоге", e);
        }
    }

    private static IllegalStateException invalid(int line) {
        return new IllegalStateException("Некорректная строка " + line + " в .env: ожидается KEY=VALUE с парными кавычками");
    }
}
