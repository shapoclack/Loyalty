package org.example.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

/** Ввод с проверкой формата и повтором запроса при ошибке. */
public class ConsoleIO {

    /** Поток ввода закрыт (Ctrl+D) — программа завершается. */
    public static class InputClosedException extends RuntimeException {
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BufferedReader in;
    private final PrintStream out;

    public ConsoleIO() {
        this.in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        this.out = System.out;
    }

    public void println() {
        out.println();
    }

    public void println(String text) {
        out.println(text);
    }

    public void printf(String format, Object... args) {
        out.printf(format, args);
    }

    public void success(String text) {
        out.println("✔ " + text);
    }

    public void error(String text) {
        out.println("✖ " + text);
    }

    public String line(String prompt) {
        out.print(prompt + ": ");
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                throw new InputClosedException();
            }
            return line.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String text(String prompt) {
        while (true) {
            String value = line(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            error("Значение обязательно");
        }
    }

    /** Пустой ввод -> null. */
    public String optionalText(String prompt) {
        String value = line(prompt + " (Enter — пропустить)");
        return value.isEmpty() ? null : value;
    }

    public int integer(String prompt) {
        while (true) {
            String value = line(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                error("Введите целое число");
            }
        }
    }

    public BigDecimal decimal(String prompt) {
        while (true) {
            BigDecimal value = parseDecimal(line(prompt));
            if (value != null) {
                return value;
            }
            error("Введите число, например 1500 или 99.90");
        }
    }

    public BigDecimal optionalDecimal(String prompt) {
        while (true) {
            String raw = line(prompt + " (Enter — пропустить)");
            if (raw.isEmpty()) {
                return null;
            }
            BigDecimal value = parseDecimal(raw);
            if (value != null) {
                return value;
            }
            error("Введите число, например 1500 или 99.90");
        }
    }

    public LocalDate optionalDate(String prompt) {
        while (true) {
            String raw = line(prompt + " [ДД.ММ.ГГГГ] (Enter — пропустить)");
            if (raw.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(raw, DATE);
            } catch (DateTimeParseException e) {
                error("Неверная дата, пример: 31.12.1990");
            }
        }
    }

    public boolean confirm(String prompt) {
        String answer = line(prompt + " [д/н]").toLowerCase();
        return answer.startsWith("д") || answer.startsWith("y");
    }

    public <T> T choose(String prompt, List<T> options, Function<T, String> label) {
        for (int i = 0; i < options.size(); i++) {
            out.printf("  %d. %s%n", i + 1, label.apply(options.get(i)));
        }
        while (true) {
            int index = integer(prompt);
            if (index >= 1 && index <= options.size()) {
                return options.get(index - 1);
            }
            error("Выберите номер от 1 до " + options.size());
        }
    }

    /** Выбор варианта или «любой» (0) — тогда возвращается null. */
    public <T> T chooseOrAny(String prompt, List<T> options, Function<T, String> label) {
        out.println("  0. любой");
        for (int i = 0; i < options.size(); i++) {
            out.printf("  %d. %s%n", i + 1, label.apply(options.get(i)));
        }
        while (true) {
            int index = integer(prompt);
            if (index == 0) {
                return null;
            }
            if (index >= 1 && index <= options.size()) {
                return options.get(index - 1);
            }
            error("Выберите номер от 0 до " + options.size());
        }
    }

    private static BigDecimal parseDecimal(String raw) {
        try {
            return new BigDecimal(raw.replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
