package com.studylab.gui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashScreen {

    public interface SplashFinishedListener {
        void onFinished();
    }

    public static void show(Stage stage, SplashFinishedListener listener) {

        Label logo = new Label("STUDY LAB");
        logo.setStyle("-fx-font-size: 46px; -fx-font-weight: bold; -fx-text-fill: linear-gradient(#00eaff,#009dff);");
        logo.setFont(Font.font("Segoe UI"));

        Label sub = new Label("Accelerate your learning");
        sub.setStyle("-fx-text-fill: #a8f0ff; -fx-font-size: 14px;");

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");
        root.getChildren().addAll(logo, sub);

        StackPane.setAlignment(logo, Pos.CENTER);
        StackPane.setAlignment(sub, Pos.BOTTOM_CENTER);
        sub.setTranslateY(-40);

        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.show();

        FadeTransition ft = new FadeTransition(Duration.seconds(2), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setCycleCount(1);

        ft.setOnFinished(ev -> listener.onFinished());
        ft.play();
    }
}
