package com.studylab.updater;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UpdateProgressWindow {

    private Stage window;
    private ProgressBar progressBar;
    private Label lblStatus;
    private Button btnCancel;

    private volatile boolean cancelled = false;

    public UpdateProgressWindow() {
        Platform.runLater(() -> {
            window = new Stage();
            window.setTitle("Atualizando StudyLab...");
            window.initModality(Modality.APPLICATION_MODAL);
            window.setResizable(false);

            lblStatus = new Label("Iniciando download...");
            progressBar = new ProgressBar(0);

            btnCancel = new Button("Cancelar");
            btnCancel.setOnAction(e -> cancelled = true);

            VBox root = new VBox(15, lblStatus, progressBar, btnCancel);
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);

            window.setScene(new Scene(root, 350, 150));
            window.show();
        });
    }

    public void updateProgress(double progress, String message) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            lblStatus.setText(message);
        });
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void close() {
        Platform.runLater(() -> window.close());
    }
}
