package com.studylab.updater;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class AutoUpdater {

    private static final String CURRENT_VERSION = "v1.0.0";
    private static final String REPO_API = "https://api.github.com/repos/JoaoMPrestes/StudyLab/releases/latest";

    public static void checkForUpdates() {
        new Thread(() -> {
            try {
                String latest = VersionChecker.getLatestVersion(REPO_API);
                if (latest == null) return;

                if (VersionChecker.isNewerVersion(latest, CURRENT_VERSION)) {
                    Platform.runLater(() -> promptAndUpdate(latest));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "AutoUpdater-Thread").start();
    }

    private static void promptAndUpdate(String latestVersion) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Atualização disponível");
        alert.setHeaderText("Nova versão disponível: " + latestVersion);
        alert.setContentText("Deseja baixar e instalar agora?");

        ButtonType btnYes = new ButtonType("Sim", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Não", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        Optional<ButtonType> resp = alert.showAndWait();
        if (resp.isPresent() && resp.get() == btnYes) {
            new Thread(() -> {
                try {
                    downloadAndRunInstallerWithProgress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "AutoUpdater-Downloader").start();
        }
    }

    /**
     * NOVO MÉTODO → com janela e progress bar
     */
    private static void downloadAndRunInstallerWithProgress() throws Exception {

        String downloadUrl = VersionChecker.getInstallerUrl(REPO_API);
        if (downloadUrl == null) {
            System.out.println("Nenhum instalador .exe encontrado na release.");
            return;
        }

        // === Criar janela com ProgressBar ===
        final Stage progressStage = new Stage();
        Platform.runLater(() -> {
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(350);

            Label label = new Label("Baixando atualização...");

            VBox root = new VBox(15, label, progressBar);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");
            root.setPrefWidth(380);

            progressStage.initModality(Modality.APPLICATION_MODAL);
            progressStage.setTitle("Atualizando StudyLab");
            progressStage.setScene(new Scene(root));
            progressStage.setResizable(false);
            progressStage.show();

            // Thread de download real
            new Thread(() -> {
                try {
                    File temp = downloadInstaller(downloadUrl, progress -> {
                        Platform.runLater(() -> progressBar.setProgress(progress));
                    });

                    Platform.runLater(progressStage::close);

                    // Executar instalador
                    ProcessBuilder pb = new ProcessBuilder(temp.getAbsolutePath());
                    pb.start();

                    Platform.exit();
                    System.exit(0);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(progressStage::close);
                }
            }).start();
        });
    }

    /**
     * Baixar arquivo e atualizar a progress bar
     */
    private static File downloadInstaller(String downloadUrl, ProgressCallback callback) throws Exception {

        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setRequestProperty("User-Agent", "StudyLab-Updater");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);

        int fileSize = conn.getContentLength();

        File temp = new File(System.getProperty("java.io.tmpdir"), "StudyLab-Updater.exe");

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(temp)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                if (fileSize > 0) {
                    double progress = (double) totalRead / fileSize;
                    callback.update(progress);
                }
            }
        }

        return temp;
    }

    @FunctionalInterface
    interface ProgressCallback {
        void update(double progress);
    }
}
