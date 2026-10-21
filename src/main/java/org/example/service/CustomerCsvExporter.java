package org.example.service;

import org.example.service.DashboardService.CustomerRow;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** CSV текущей выборки; разделитель ;, BOM для корректного открытия кириллицы в Excel. */
public final class CustomerCsvExporter {
    public void export(Path file, List<CustomerRow> rows) throws IOException {
        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            writer.write("ФИО;Телефон;Email;Дата рождения;Статус;Уровни карт;Бонусный баланс;Дата регистрации\r\n");
            for (var row : rows) {
                var c = row.customer();
                String[] values = {c.fullName(), c.phone(), c.email(),
                        c.birthDate() == null ? "" : c.birthDate().toString(),
                        c.isActive() ? "Активен" : "Заблокирован", row.tiers(), row.balance().toPlainString(),
                        c.createdAt().toString()};
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) writer.write(';');
                    writer.write(cell(values[i]));
                }
                writer.write("\r\n");
            }
        }
    }
    static String cell(String value) {
        String text = value == null ? "" : value;
        // Текст клиента не должен интерпретироваться табличным редактором как формула.
        if (text.stripLeading().matches("^[=+@-].*")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
