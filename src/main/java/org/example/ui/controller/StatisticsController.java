package org.example.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.service.DashboardService;
import org.example.service.StatisticsService;
import org.example.ui.support.Ui;

public final class StatisticsController {
    @FXML private DatePicker from, to;
    @FXML private TilePane metrics;
    @FXML private Label error;
    private final DashboardService.Snapshot snapshot;
    public StatisticsController(DashboardService.Snapshot snapshot) { this.snapshot = snapshot; }
    @FXML private void initialize() { Ui.dates(from, to); calculate(); }
    @FXML private void calculate() {
        try {
            var values = new StatisticsService().calculate(snapshot, Ui.date(from, "Начало периода"), Ui.date(to, "Конец периода"));
            metrics.getChildren().clear();
            values.forEach((title, value) -> {
                Label number = new Label(value); number.getStyleClass().add("metric-value");
                Label label = new Label(title); label.setWrapText(true);
                VBox card = new VBox(8, number, label); card.getStyleClass().add("metric");
                card.setPrefWidth(235); card.setMinHeight(100); metrics.getChildren().add(card);
            });
            error.setText("");
        } catch (RuntimeException e) { error.setText(Ui.message(e)); }
    }
    @FXML private void reset() { from.setValue(null); to.setValue(null); calculate(); }
}
