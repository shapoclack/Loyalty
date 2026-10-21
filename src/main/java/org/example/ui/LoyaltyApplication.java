package org.example.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.AppContext;
import org.example.config.DbConfig;
import org.example.db.Database;
import org.example.service.DashboardService;
import org.example.ui.controller.MainController;
import org.example.ui.support.Ui;

public final class LoyaltyApplication extends Application {
    @Override public void start(Stage stage) {
        Database db = new Database(DbConfig.load());
        AppContext context = new AppContext(db);
        MainController controller = new MainController(context, new DashboardService(db));
        Scene scene = new Scene(Ui.load("main.fxml", controller), 1180, 740);
        scene.getStylesheets().add(getClass().getResource("/org/example/ui/style.css").toExternalForm());
        stage.setTitle("Loyalty — программа лояльности");
        stage.setMinWidth(960); stage.setMinHeight(600);
        stage.setScene(scene); stage.show();
        controller.refresh();
    }
    @Override public void stop() { Ui.stop(); }
    public static void main(String[] args) { launch(args); }
}
