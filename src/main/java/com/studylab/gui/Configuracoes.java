package com.studylab.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class Configuracoes {

    public interface ConfigSavedListener {
        void onSaved(ConfiguracoesData cfg);
    }

    private static final Map<String, String> THEMES = new LinkedHashMap<>();
    static {
        THEMES.put("Dark (default)", "dark");
        THEMES.put("Light", "light");
        THEMES.put("Neon Blue", "neon");
        THEMES.put("Ocean", "ocean");
        THEMES.put("Matrix Green", "matrix");
        THEMES.put("Dracula", "dracula");
        THEMES.put("Solarized Dark", "solarized-dark");
        THEMES.put("Solarized Light", "solarized-light");
        THEMES.put("Custom (use ColorPickers)", "custom");
    }

    public static void show(Stage stage, ConfigSavedListener listener) {

        // 🔥 Carrega configs salvas
        ConfiguracoesData cfgSaved = ConfigManager.load();

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ----------------- TAB 1: THEMES -----------------
        VBox temaBox = new VBox(12);
        temaBox.setPadding(new Insets(16));
        temaBox.setAlignment(Pos.TOP_CENTER);

        Label temaTitle = new Label("Choose a theme");
        temaTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        ComboBox<String> cbThemes = new ComboBox<>();
        cbThemes.getItems().addAll(THEMES.keySet());

        // define tema selecionado atual
        cbThemes.getSelectionModel().select(
                THEMES.entrySet().stream()
                        .filter(e -> e.getValue().equals(cfgSaved.temaSelecionado))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("Dark (default)")
        );

        Button btnApplyTheme = new Button("Apply Theme (Preview)");

        temaBox.getChildren().addAll(temaTitle, cbThemes, btnApplyTheme);

        Tab tabThemes = new Tab("Themes", temaBox);

        // ----------------- TAB 2: CUSTOM -----------------
        VBox customBox = new VBox(10);
        customBox.setPadding(new Insets(16));
        customBox.setAlignment(Pos.TOP_CENTER);

        Label customTitle = new Label("Custom Theme - live preview");
        customTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ColorPicker cpBackground = new ColorPicker(Color.web(cfgSaved.customBackground));
        ColorPicker cpPrimary = new ColorPicker(Color.web(cfgSaved.customPrimary));
        ColorPicker cpTextManual = new ColorPicker(Color.web(cfgSaved.customText));

        CheckBox chkOverrideText = new CheckBox("Override text color (manual)");
        chkOverrideText.setSelected(cfgSaved.customOverrideText);

        HBox cps = new HBox(12,
                new VBox(new Label("Background"), cpBackground),
                new VBox(new Label("Primary"), cpPrimary),
                new VBox(new Label("Text (manual)"), cpTextManual)
        );
        cps.setAlignment(Pos.CENTER);

        Button btnApplyCustom = new Button("Apply Custom (Preview)");

        customBox.getChildren().addAll(customTitle, cps, chkOverrideText, btnApplyCustom);

        Tab tabCustom = new Tab("Custom", customBox);

        // ----------------- TAB 3: QUIZ OPTIONS -----------------
        VBox optionsBox = new VBox(12);
        optionsBox.setPadding(new Insets(16));
        optionsBox.setAlignment(Pos.TOP_CENTER);

        CheckBox chkRandomQuestoes = new CheckBox("Randomizar ordem das questões");
        chkRandomQuestoes.setSelected(cfgSaved.randomizarQuestoes);

        CheckBox chkRandomAlternativas = new CheckBox("Randomizar ordem das alternativas");
        chkRandomAlternativas.setSelected(cfgSaved.randomizarAlternativas);

        CheckBox chkMostrarCorrecao = new CheckBox("Mostrar correção após responder");
        chkMostrarCorrecao.setSelected(cfgSaved.mostrarCorrecao);

        CheckBox chkPermitirRevisao = new CheckBox("Permitir revisão das erradas no final");
        chkPermitirRevisao.setSelected(cfgSaved.permitirRevisao);

        CheckBox chkSons = new CheckBox("Sons de acerto/erro");
        chkSons.setSelected(cfgSaved.usarSons);

        CheckBox chkModoProva = new CheckBox("Ativar modo prova (tempo por questão)");
        chkModoProva.setSelected(cfgSaved.modoProva);

        Label lblTempo = new Label("Tempo por questão: " + cfgSaved.tempoPorQuestao + "s");

        Slider sliderTempo = new Slider(3, 20, cfgSaved.tempoPorQuestao);
        sliderTempo.setShowTickLabels(true);
        sliderTempo.setShowTickMarks(true);
        sliderTempo.valueProperty().addListener((obs, ov, nv) ->
                lblTempo.setText("Tempo por questão: " + nv.intValue() + "s")
        );

        optionsBox.getChildren().addAll(
                chkRandomQuestoes,
                chkRandomAlternativas,
                chkMostrarCorrecao,
                chkPermitirRevisao,
                chkSons,
                chkModoProva,
                lblTempo,
                sliderTempo
        );

        Tab tabOptions = new Tab("Quiz Options", optionsBox);

        tabs.getTabs().addAll(tabThemes, tabCustom, tabOptions);

        // ----------------- FOOTER -----------------
        Button btnSalvar = new Button("Save Settings and Choose File");
        btnSalvar.setStyle("-fx-font-size: 16px; -fx-padding: 8 12;");
        btnSalvar.setDefaultButton(true);

        VBox root = new VBox(12, tabs, btnSalvar);
        root.setPadding(new Insets(14));
        root.setAlignment(Pos.TOP_CENTER);

        Scene sc = new Scene(root, 700, 520);

        btnApplyTheme.setOnAction(ev -> {
            String selected = cbThemes.getSelectionModel().getSelectedItem();
            applyThemePreview(sc, THEMES.get(selected), null, null, false, null);
        });

        btnApplyCustom.setOnAction(ev -> {
            applyThemePreview(
                    sc,
                    "custom",
                    toHex(cpBackground.getValue()),
                    toHex(cpPrimary.getValue()),
                    chkOverrideText.isSelected(),
                    toHex(cpTextManual.getValue())
            );
            cbThemes.getSelectionModel().select("Custom (use ColorPickers)");
        });

        // ----------------- SAVE CONFIGS -----------------
        btnSalvar.setOnAction(ev -> {

            ConfiguracoesData cfg = new ConfiguracoesData();

            cfg.randomizarQuestoes = chkRandomQuestoes.isSelected();
            cfg.randomizarAlternativas = chkRandomAlternativas.isSelected();
            cfg.mostrarCorrecao = chkMostrarCorrecao.isSelected();
            cfg.permitirRevisao = chkPermitirRevisao.isSelected();
            cfg.usarSons = chkSons.isSelected();
            cfg.modoProva = chkModoProva.isSelected();
            cfg.tempoPorQuestao = (int) sliderTempo.getValue();

            String chosen = cbThemes.getSelectionModel().getSelectedItem();
            cfg.temaSelecionado = THEMES.get(chosen);

            if ("custom".equals(cfg.temaSelecionado)) {
                cfg.customBackground = toHex(cpBackground.getValue());
                cfg.customPrimary = toHex(cpPrimary.getValue());
                cfg.customOverrideText = chkOverrideText.isSelected();
                if (cfg.customOverrideText)
                    cfg.customText = toHex(cpTextManual.getValue());
            }

            // 🔥 Salva!
            ConfigManager.save(cfg);

            listener.onSaved(cfg);
        });

        stage.setScene(sc);
        stage.setTitle("STUDY LAB — Settings");
        stage.show();
    }

    private static void applyThemePreview(Scene scene, String themeId,
                                          String bgHex, String primHex,
                                          boolean overrideText, String textHex) {

        scene.getStylesheets().clear();

        try {
            String base = Configuracoes.class.getResource("/styles/style-base.css").toExternalForm();
            scene.getStylesheets().add(base);

        } catch (Exception ignored) {}

        if (!"custom".equals(themeId)) return;

        String style = "-fx-background-color: " + bgHex + ";";

        scene.getRoot().setStyle(style);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                Math.round(c.getRed() * 255),
                Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255));
    }
}
