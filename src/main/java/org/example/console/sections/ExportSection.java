package org.example.console.sections;

import org.example.console.ConsoleIO;
import org.example.console.Menu;
import org.example.console.MenuSection;
import org.example.service.ExportService;

import java.nio.file.Path;

public class ExportSection implements MenuSection {

    private final ConsoleIO io;
    private final ExportService exports;

    public ExportSection(ConsoleIO io, ExportService exports) {
        this.io = io;
        this.exports = exports;
    }

    @Override
    public String title() {
        return "Экспорт в Excel";
    }

    @Override
    public void fill(Menu menu) {
        menu.add("Выгрузить всю базу", () -> report(exports.exportAll()))
                .add("Выгрузить историю счёта по карте", () -> report(exports.exportAccount(io.text("Номер карты"))));
    }

    private void report(Path file) {
        io.success("Файл сохранён: " + file);
    }
}
