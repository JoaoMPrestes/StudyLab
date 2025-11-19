package com.studylab.gui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private static final String DIR =
            System.getProperty("user.home") + File.separator + ".studylab";

    private static final String FILE =
            DIR + File.separator + "config.json";

    // ===========================
    // SAVE
    // ===========================
    public static void save(ConfiguracoesData cfg) {
        try {
            File d = new File(DIR);
            if (!d.exists()) d.mkdirs();

            File f = new File(FILE);
            try (BufferedWriter w = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {

                String json = "{\n" +
                        "  \"randomizarQuestoes\": " + cfg.randomizarQuestoes + ",\n" +
                        "  \"randomizarAlternativas\": " + cfg.randomizarAlternativas + ",\n" +
                        "  \"mostrarCorrecao\": " + cfg.mostrarCorrecao + ",\n" +
                        "  \"permitirRevisao\": " + cfg.permitirRevisao + ",\n" +
                        "  \"usarSons\": " + cfg.usarSons + ",\n" +
                        "  \"modoProva\": " + cfg.modoProva + ",\n" +
                        "  \"tempoPorQuestao\": " + cfg.tempoPorQuestao + ",\n" +
                        "  \"temaSelecionado\": \"" + cfg.temaSelecionado + "\",\n" +
                        "  \"customBackground\": \"" + cfg.customBackground + "\",\n" +
                        "  \"customPrimary\": \"" + cfg.customPrimary + "\",\n" +
                        "  \"customOverrideText\": " + cfg.customOverrideText + ",\n" +
                        "  \"customText\": \"" + cfg.customText + "\"\n" +
                        "}";

                w.write(json);
                w.flush();
            }

            System.out.println("[Config] Saved at " + FILE);

        } catch (Exception ex) {
            System.err.println("[Config] Save error: " + ex.getMessage());
        }
    }

    // ===========================
    // LOAD
    // ===========================
    public static ConfiguracoesData load() {
        try {
            File f = new File(FILE);
            if (!f.exists()) {
                System.out.println("[Config] No config found — using defaults.");
                return new ConfiguracoesData();
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null)
                    sb.append(line).append("\n");
            }

            String json = sb.toString();
            return parseJson(json);

        } catch (Exception ex) {
            System.err.println("[Config] Load error: " + ex.getMessage());
            return new ConfiguracoesData();
        }
    }

    // ===========================
    // PARSE JSON (REGEX)
    // ===========================
    private static ConfiguracoesData parseJson(String raw) {
        ConfiguracoesData cfg = new ConfiguracoesData();

        cfg.randomizarQuestoes = getBool(raw, "randomizarQuestoes", false);
        cfg.randomizarAlternativas = getBool(raw, "randomizarAlternativas", false);
        cfg.mostrarCorrecao = getBool(raw, "mostrarCorrecao", false);
        cfg.permitirRevisao = getBool(raw, "permitirRevisao", true);
        cfg.usarSons = getBool(raw, "usarSons", false);
        cfg.modoProva = getBool(raw, "modoProva", false);
        cfg.tempoPorQuestao = getInt(raw, "tempoPorQuestao", 8);

        cfg.temaSelecionado = getString(raw, "temaSelecionado", "dark");
        cfg.customBackground = getString(raw, "customBackground", "#0e0f12");
        cfg.customPrimary = getString(raw, "customPrimary", "#00eaff");
        cfg.customOverrideText = getBool(raw, "customOverrideText", false);
        cfg.customText = getString(raw, "customText", "#ffffff");

        return cfg;
    }

    // ===========================
    // HELPERS
    // ===========================
    private static boolean getBool(String raw, String key, boolean def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)").matcher(raw);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : def;
    }

    private static int getInt(String raw, String key, int def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(raw);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }

    private static String getString(String raw, String key, String def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(raw);
        return m.find() ? m.group(1) : def;
    }
}
