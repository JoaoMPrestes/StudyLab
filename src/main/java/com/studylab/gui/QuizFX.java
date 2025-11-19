package com.studylab.gui;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.studylab.updater.AutoUpdater;
import com.studylab.gui.ConfiguracoesData;
import com.studylab.gui.Configuracoes;
import com.studylab.gui.SplashScreen;
import com.studylab.gui.LicenseManager;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class QuizFX extends Application {

    private ConfiguracoesData configuracoes = new ConfiguracoesData();

    // --- Estrutura ---
    static class Pergunta {
        String texto;
        List<String> alternativas;
        String correta;
        Pergunta(String t, List<String> a, String c) {
            texto = t; alternativas = a; correta = c;
        }
    }

    private List<Pergunta> perguntas = new ArrayList<>();
    private List<Integer> indicesErradas = new ArrayList<>();
    private int indice = 0;
    private int acertos = 0;

    private Label lblTitulo;
    private StackPane progressStack;
    private ProgressBar progressBar;
    private Label lblTimeOnBar;
    private Label lblProgresso;
    private Label lblPergunta;
    private VBox vboxAlternativas;
    private ToggleGroup grupo;
    private Button btnProximo;

    private Timeline progressTimeline;
    private Timeline clockTimeline;
    private long secondsElapsed = 0;
    private Instant startInstant;
    private boolean emModoRevisao = false;

    private List<Pergunta> perguntasBackup;
    private List<Integer> backupIndicesMap;

    private int BAR_SECONDS = 8;

    private LicenseManager licenseManager;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
    	AutoUpdater.checkForUpdates();
        stage.setTitle("Study Power - Quiz");

        licenseManager = new LicenseManager();

        SplashScreen.show(stage, () -> {

            // 🔥 Carrega configurações antes de abrir a tela
            Configuracoes.show(stage, cfg -> {

                this.configuracoes = cfg;

                // aplica tempo se modo prova
                if (configuracoes.modoProva)
                    BAR_SECONDS = configuracoes.tempoPorQuestao;

                // Sistema de Licença
                boolean ok = licenseManager.acquireLicense();
                if (!ok) {
                    mostrarAlertaLicenca();
                    Platform.exit();
                    return;
                }

                iniciarFluxoDoQuiz(stage);
            });
        });
    }

    private void mostrarAlertaLicenca() {
        String ownerEmail = System.getenv().getOrDefault("NOTIFY_EMAIL_TO", "");
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Licenses unavailable");
        a.setHeaderText("No licenses available right now.");

        String content = "All licenses are currently in use. You can request access from the administrator.";
        if (!ownerEmail.isEmpty()) {
            content += "\n\nClick OK to open your mail client with a pre-filled request.";
            a.setContentText(content);

            Optional<ButtonType> res = a.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                try {
                    String subj = URLEncoder.encode("StudyLab access request", "UTF-8");
                    String body = URLEncoder.encode(
                            "Please grant access. My device ID: " + licenseManager.getLocalId(), "UTF-8");
                    String mailto = "mailto:" + ownerEmail + "?subject=" + subj + "&body=" + body;
                    Desktop.getDesktop().mail(new URI(mailto));
                } catch (Exception ex) {}
            }

        } else {
            a.setContentText(content + "\n\nNo administrator email configured.");
            a.showAndWait();
        }
    }

    private void iniciarFluxoDoQuiz(Stage stage) {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecione o arquivo de perguntas (UTF-8)");
        File file = chooser.showOpenDialog(stage);

        if (file == null) {
            try { licenseManager.releaseLicense(); } catch (Exception ignored) {}
            Platform.exit();
            return;
        }

        if (!carregarPerguntas(file)) {
            new Alert(Alert.AlertType.ERROR, "Erro ao ler o arquivo ou arquivo vazio.")
                    .showAndWait();
            try { licenseManager.releaseLicense(); } catch (Exception ignored) {}
            Platform.exit();
            return;
        }

        if (configuracoes.randomizarQuestoes)
            Collections.shuffle(perguntas);

        // --- UI ---
        lblTitulo = new Label("QUIZ - Simulado para Estudos");
        lblTitulo.getStyleClass().add("title");

        lblProgresso = new Label("0 / " + perguntas.size());
        lblProgresso.getStyleClass().add("progress");

        progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(420);
        progressBar.getStyleClass().add("progress-bar");

        lblTimeOnBar = new Label("00:00");
        lblTimeOnBar.getStyleClass().add("time-on-bar");

        progressStack = new StackPane(progressBar, lblTimeOnBar);
        progressStack.setPadding(new Insets(6));

        Region spacer = new Region();

        HBox topo = new HBox(16, lblTitulo, spacer, lblProgresso, progressStack);
        topo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topo.setPadding(new Insets(12));

        lblPergunta = new Label();
        lblPergunta.setWrapText(true);
        lblPergunta.getStyleClass().add("question");
        lblPergunta.setMaxWidth(900);

        vboxAlternativas = new VBox(12);
        grupo = new ToggleGroup();

        btnProximo = new Button("Próximo");
        btnProximo.getStyleClass().add("primary");
        btnProximo.setOnAction(e -> onProximo());

        HBox rodape = new HBox(btnProximo);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        rodape.setPadding(new Insets(12));

        VBox centro = new VBox(18, lblPergunta, vboxAlternativas, rodape);
        centro.setPadding(new Insets(18));

        BorderPane root = new BorderPane();
        root.setTop(topo);
        root.setCenter(centro);
        root.getStyleClass().add("root");

        Label footer = new Label("For educational purposes only — use responsibly.");
        footer.getStyleClass().add("footer-text");

        HBox footerBox = new HBox(footer);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.setPadding(new Insets(8));

        root.setBottom(footerBox);

        Scene scene = new Scene(root, 1100, 700);

        aplicarTema(scene);

        stage.setScene(scene);
        stage.show();

        iniciarCronometroGlobal();
        criarAnimacaoBarra();
        mostrarPergunta();
    }

    // ------------------------------------------------------
    // 🔥 CARREGAR TEMA + CSS CUSTOM
    // ------------------------------------------------------
    private void aplicarTema(Scene scene) {

        scene.getStylesheets().clear();

        try {
            String base = QuizFX.class.getResource("/styles/style-base.css").toExternalForm();
            scene.getStylesheets().add(base);
        } catch (Exception ignored) {}

        // Tema normal
        if (!"custom".equals(configuracoes.temaSelecionado)) {

            String[] tryPaths = {
                    "/gui.themes/" + configuracoes.temaSelecionado + ".css",
                    "gui.themes/" + configuracoes.temaSelecionado + ".css",
                    "themes/" + configuracoes.temaSelecionado + ".css"
            };

            for (String p : tryPaths) {
                try {
                    var url = QuizFX.class.getResource(
                            p.startsWith("/") ? p : "/" + p
                    );
                    if (url != null) {
                        scene.getStylesheets().add(url.toExternalForm());
                        return;
                    }
                } catch (Exception ignored) {}
            }
            return;
        }

        // --- CUSTOM THEME ---
        StringBuilder css = new StringBuilder();
        css.append(".root { -fx-background-color: ")
                .append(configuracoes.customBackground)
                .append("; }\n");

        css.append(".title { -fx-text-fill: ")
                .append(configuracoes.customOverrideText ? configuracoes.customText : configuracoes.customPrimary)
                .append("; }\n");

        css.append(".question { -fx-text-fill: ")
                .append(configuracoes.customOverrideText ? configuracoes.customText : configuracoes.customPrimary)
                .append("; }\n");

        css.append(".alt-text { -fx-text-fill: ")
                .append(configuracoes.customOverrideText ? configuracoes.customText : configuracoes.customPrimary)
                .append("; }\n");

        css.append(".progress-bar > .bar { -fx-background-color: ")
                .append(configuracoes.customPrimary)
                .append("; }\n");

        // Converte para data url (CSS inline)
        String encoded = "data:text/css," + css.toString().replace("#", "%23").replace("\n", "%0A");
        scene.getStylesheets().add(encoded);
    }

    // ------------------------------------------------------

    private boolean carregarPerguntas(File arquivo) {
        perguntas.clear();

        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8))) {

            String linha, acumulador = "";

            while ((linha = br.readLine()) != null) {
                linha = linha.replace("\t", "").trim();
                if (linha.isEmpty()) continue;

                if (!acumulador.isEmpty()) acumulador += " " + linha;
                else acumulador = linha;

                String[] partes = acumulador.split("\\|");
                if (partes.length < 3) continue;

                String enunciado = partes[0].replace("\\n", "\n").trim();

                List<String> alternativas = new ArrayList<>();
                for (int i = 1; i < partes.length - 1; i++)
                    alternativas.add(partes[i].trim());

                perguntas.add(new Pergunta(enunciado, alternativas, partes[partes.length - 1].trim()));
                acumulador = "";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return !perguntas.isEmpty();
    }

    private void iniciarCronometroGlobal() {
        startInstant = Instant.now();
        secondsElapsed = 0;
        lblTimeOnBar.setText(formatSeconds(secondsElapsed));

        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            lblTimeOnBar.setText(formatSeconds(secondsElapsed));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void criarAnimacaoBarra() {
        if (progressTimeline != null) progressTimeline.stop();

        progressBar.setProgress(0);

        progressTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.seconds(BAR_SECONDS), new KeyValue(progressBar.progressProperty(), 1.0))
        );

        progressTimeline.setCycleCount(Animation.INDEFINITE);
        progressTimeline.playFromStart();
    }

    private void reiniciarBarraVisual() {
        if (progressTimeline != null) progressTimeline.stop();
        progressBar.setProgress(0.0);
        progressTimeline.playFromStart();
    }

    private void mostrarPergunta() {

        if (indice >= perguntas.size()) {
            finalizarQuiz();
            return;
        }

        Pergunta p = perguntas.get(indice);

        lblProgresso.setText((indice + 1) + " / " + perguntas.size());
        lblPergunta.setText(p.texto);

        vboxAlternativas.getChildren().clear();
        grupo = new ToggleGroup();

        List<String> alternativas = new ArrayList<>(p.alternativas);
        if (configuracoes.randomizarAlternativas)
            Collections.shuffle(alternativas);

        char letra = 'A';
        for (String alt : alternativas) {
            RadioButton rb = new RadioButton(letra + ") " + alt);
            rb.setToggleGroup(grupo);
            rb.setUserData(String.valueOf(letra));
            rb.getStyleClass().add("alt-text");
            rb.setWrapText(true);
            rb.setMaxWidth(860);

            vboxAlternativas.getChildren().add(rb);
            letra++;
        }

        reiniciarBarraVisual();
    }

    private void onProximo() {

        Toggle sel = grupo.getSelectedToggle();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione uma alternativa antes de continuar.")
                    .showAndWait();
            return;
        }

        String escolhida = sel.getUserData().toString();
        Pergunta p = perguntas.get(indice);

        boolean acertou = escolhida.equalsIgnoreCase(p.correta);

        if (acertou) acertos++;
        else indicesErradas.add(indice);

        indice++;

        if (indice >= perguntas.size()) finalizarQuiz();
        else mostrarPergunta();
    }

    private void finalizarQuiz() {

        if (progressTimeline != null) progressTimeline.stop();
        if (clockTimeline != null) clockTimeline.stop();

        long totalSeconds = secondsElapsed;

        double nota = perguntas.size() == 0 ? 0 :
                ((double) acertos / perguntas.size()) * 10.0;

        String resumo = String.format(
                "Fim do Quiz!\nPerguntas: %d\nAcertos: %d\nErros: %d\nNota: %.1f\nTempo total: %s",
                perguntas.size(), acertos, perguntas.size() - acertos,
                nota, formatSeconds(totalSeconds)
        );

        ButtonType revisar = new ButtonType("Revisar erradas");
        ButtonType sair = new ButtonType("Sair");

        Alert a = new Alert(Alert.AlertType.INFORMATION, resumo, sair, revisar);
        a.setTitle("Resultado");

        Optional<ButtonType> res = a.showAndWait();

        if (res.isPresent() && res.get() == revisar &&
                configuracoes.permitirRevisao &&
                !indicesErradas.isEmpty()) {

            iniciarRevisao();
        } else {
            try { licenseManager.releaseLicense(); } catch (Exception ignored) {}
            Platform.exit();
        }
    }

    private void iniciarRevisao() {

        List<Pergunta> lista = new ArrayList<>();
        backupIndicesMap = new ArrayList<>();

        for (Integer idx : indicesErradas) {
            lista.add(perguntas.get(idx));
            backupIndicesMap.add(idx);
        }

        perguntasBackup = perguntas;
        perguntas = lista;
        indice = 0;
        acertos = 0;
        indicesErradas = new ArrayList<>();
        emModoRevisao = true;

        reiniciarBarraVisual();
        mostrarPergunta();
    }

    private String formatSeconds(long secs) {
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }
}
