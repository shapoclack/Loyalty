package org.example.console;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Форматирование значений для вывода. */
public final class Fmt {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Fmt() {
    }

    public static String money(BigDecimal value) {
        return value == null ? "—" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String signed(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + money(value);
    }

    public static String number(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    public static String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    public static String text(Object value) {
        return value == null ? "—" : value.toString();
    }
}
