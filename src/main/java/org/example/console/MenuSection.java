package org.example.console;

/**
 * Раздел главного меню. Чтобы добавить новый раздел, реализуйте интерфейс
 * и добавьте экземпляр в список разделов в {@link org.example.AppContext}.
 */
public interface MenuSection {

    String title();

    void fill(Menu menu);
}
