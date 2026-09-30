package org.example.console;

import java.util.List;

/** Главное меню, собранное из разделов. */
public class LoyaltyConsole {

    private final ConsoleIO io;
    private final List<MenuSection> sections;

    public LoyaltyConsole(ConsoleIO io, List<MenuSection> sections) {
        this.io = io;
        this.sections = sections;
    }

    public void run() {
        Menu main = new Menu("Программа лояльности", io, "Выход");
        for (MenuSection section : sections) {
            main.addSubmenu(section.title(), section::fill);
        }
        try {
            main.run();
        } catch (ConsoleIO.InputClosedException e) {
            io.println();
        }
        io.println("До свидания!");
    }
}
