package org.example.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Простая текстовая таблица с выравниванием по ширине колонок. */
public class Table {

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public Table(String... headers) {
        this.headers = headers;
    }

    public Table row(Object... cells) {
        rows.add(Arrays.stream(cells).map(Fmt::text).toArray(String[]::new));
        return this;
    }

    public void print(ConsoleIO io) {
        if (rows.isEmpty()) {
            io.println("  (нет данных)");
            return;
        }
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
            for (String[] row : rows) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        io.println(format(headers, widths));
        StringBuilder separator = new StringBuilder();
        for (int width : widths) {
            separator.append("-".repeat(width + 2)).append('+');
        }
        io.println(separator.substring(0, separator.length() - 1));
        rows.forEach(row -> io.println(format(row, widths)));
    }

    private static String format(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(' ').append(String.format("%-" + widths[i] + "s", cells[i])).append(' ');
        }
        return sb.toString().stripTrailing();
    }
}
