package org.example.console;

import org.example.db.DataAccessException;
import org.example.service.LoyaltyException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Нумерованное меню: пункт 0 возвращает на уровень выше. */
public class Menu {

    private record Item(String label, Runnable action, boolean submenu) {
    }

    private final String title;
    private final ConsoleIO io;
    private final String exitLabel;
    private final List<Item> items = new ArrayList<>();

    public Menu(String title, ConsoleIO io, String exitLabel) {
        this.title = title;
        this.io = io;
        this.exitLabel = exitLabel;
    }

    public Menu(String title, ConsoleIO io) {
        this(title, io, "Назад");
    }

    public Menu add(String label, Runnable action) {
        items.add(new Item(label, action, false));
        return this;
    }

    /** Вложенное меню; {@code filler} наполняет его пунктами при каждом открытии. */
    public Menu addSubmenu(String label, Consumer<Menu> filler) {
        items.add(new Item(label, () -> {
            Menu submenu = new Menu(label, io);
            filler.accept(submenu);
            submenu.run();
        }, true));
        return this;
    }

    public void run() {
        while (true) {
            io.println();
            io.println("=== " + title + " ===");
            for (int i = 0; i < items.size(); i++) {
                io.printf("%d. %s%n", i + 1, items.get(i).label());
            }
            io.println("0. " + exitLabel);
            int choice = io.integer("Выберите пункт");
            if (choice == 0) {
                return;
            }
            if (choice < 0 || choice > items.size()) {
                io.error("Нет такого пункта");
                continue;
            }
            execute(items.get(choice - 1));
        }
    }

    private void execute(Item item) {
        if (!item.submenu()) {
            io.println();
            io.println("--- " + item.label() + " ---");
        }
        try {
            item.action().run();
        } catch (LoyaltyException e) {
            io.error(e.getMessage());
        } catch (DataAccessException e) {
            io.error("Ошибка базы данных: " + e.getMessage());
        }
    }
}
