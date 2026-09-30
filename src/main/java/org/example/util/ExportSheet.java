package org.example.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Лист выгрузки: заголовки колонок и строки значений. */
public record ExportSheet(String name, List<String> headers, List<List<Object>> rows) {

    /** Колонка: заголовок и способ получить значение из объекта. */
    public record Column<T>(String header, Function<T, Object> value) {
    }

    public static <T> Column<T> column(String header, Function<T, Object> value) {
        return new Column<>(header, value);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> ExportSheet of(String name, List<T> items, Column<T>... columns) {
        List<String> headers = Arrays.stream(columns).map(Column::header).toList();
        List<List<Object>> rows = new ArrayList<>(items.size());
        for (T item : items) {
            List<Object> row = new ArrayList<>(columns.length);
            for (Column<T> column : columns) {
                row.add(column.value().apply(item));
            }
            rows.add(row);
        }
        return new ExportSheet(name, headers, rows);
    }
}
