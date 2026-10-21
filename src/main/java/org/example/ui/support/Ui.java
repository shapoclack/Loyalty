package org.example.ui.support;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.example.exception.BusinessException;
import org.example.exception.DataAccessException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/** Общая инфраструктура FXML, фоновых задач и сообщений. */
public final class Ui {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "loyalty-database");
        thread.setDaemon(true);
        return thread;
    });
    private Ui() { }
    public static Parent load(String file, Object controller) {
        FXMLLoader loader = new FXMLLoader(Ui.class.getResource("/org/example/ui/" + file));
        loader.setController(controller);
        try { return loader.load(); }
        catch (IOException e) { throw new IllegalStateException("Не удалось открыть окно " + file, e); }
    }
    public static Stage window(Window owner, String title, Parent root) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Ui.class.getResource("/org/example/ui/style.css").toExternalForm());
        stage.setScene(scene);
        return stage;
    }
    public static <T> void async(Parent root, Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        root.setDisable(true);
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(e -> {
            root.setDisable(false);
            success.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            root.setDisable(false);
            failure.accept(task.getException());
        });
        WORKER.submit(task);
    }
    public static void stop() { WORKER.shutdownNow(); }
    public static String message(Throwable error) {
        if (error instanceof BusinessException) return error.getMessage();
        if (error instanceof DataAccessException) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql) {
                    if ("23505".equals(sql.getSQLState())) return "Такое уникальное значение уже существует. Проверьте телефон или номер операции.";
                    if ("23503".equals(sql.getSQLState())) return "Запись связана с другими данными. Обновите список; запись с историей нельзя удалить.";
                }
            }
            return "Не удалось выполнить запрос к базе данных. Проверьте подключение PostgreSQL и настройки LOYALTY_DB_*. Затем повторите действие.";
        }
        if (error instanceof IOException) return "Не удалось сохранить файл. Проверьте права доступа и свободное место.";
        return "Не удалось выполнить действие. Обновите данные и повторите попытку.";
    }
    public static void error(Window owner, Throwable error) { alert(owner, Alert.AlertType.ERROR, "Ошибка", message(error)); }
    public static void info(Window owner, String text) { alert(owner, Alert.AlertType.INFORMATION, "Готово", text); }
    private static void alert(Window owner, Alert.AlertType type, String title, String text) {
        Alert alert = new Alert(type, text, ButtonType.OK);
        alert.initOwner(owner); alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }
    public static boolean confirm(Window owner, String text) {
        ButtonType delete = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, delete, ButtonType.CANCEL);
        alert.initOwner(owner); alert.setTitle("Подтверждение удаления"); alert.setHeaderText("Удалить клиента?");
        return alert.showAndWait().filter(delete::equals).isPresent();
    }
    public static String status(Enum<?> value) {
        return switch (value.name()) {
            case "ACTIVE" -> "Активен";
            case "BLOCKED" -> "Заблокирован";
            case "CLOSED" -> "Закрыт";
            case "ACCRUAL" -> "Начисление";
            case "REDEMPTION" -> "Списание";
            case "PURCHASE" -> "Покупка";
            default -> value.name();
        };
    }
    public static String format(Object value) {
        if (value == null) return "—";
        if (value instanceof LocalDate date) return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        if (value instanceof LocalDateTime date) return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        if (value instanceof BigDecimal number) return number.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        if (value instanceof Enum<?> enumeration) return status(enumeration);
        return value.toString();
    }
    /** Значения остаются типизированными, поэтому даты и суммы сортируются корректно. */
    public static <T, V> TableColumn<T, V> column(TableView<T> table, String title, Function<T, V> value, double width) {
        TableColumn<T, V> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(V item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : format(item));
            }
        });
        column.setPrefWidth(width);
        table.getColumns().add(column);
        return column;
    }
    public static void placeholder(TableView<?> table, String text) { table.setPlaceholder(new Label(text)); }
    public static void dates(DatePicker... pickers) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")
                .withResolverStyle(java.time.format.ResolverStyle.STRICT);
        for (DatePicker picker : pickers) {
            picker.setConverter(new StringConverter<>() {
                @Override public String toString(LocalDate date) { return date == null ? "" : formatter.format(date); }
                @Override public LocalDate fromString(String text) {
                    return text == null || text.isBlank() ? null : LocalDate.parse(text.strip(), formatter);
                }
            });
        }
    }
    public static LocalDate date(DatePicker picker, String field) {
        if (picker.getEditor().getText().isBlank()) return null;
        try { return picker.getConverter().fromString(picker.getEditor().getText()); }
        catch (RuntimeException e) { throw new BusinessException(field + ": введите существующую дату в формате дд.мм.гггг"); }
    }
}
