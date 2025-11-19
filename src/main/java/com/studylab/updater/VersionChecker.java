package com.studylab.updater;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility minimalista para buscar a release mais recente no GitHub
 * (usa parsing simples de JSON textual — evita dependências externas).
 *
 * URL usada: https://api.github.com/repos/JoaoMPrestes/StudyLab/releases/latest
 */
public class VersionChecker {

    /**
     * Busca o JSON da release e retorna o valor de "tag_name" (ex: "v1.0.1").
     */
    public static String getLatestVersion(String apiUrl) throws Exception {
        String json = httpGet(apiUrl);
        String tag = findJsonStringValue(json, "\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        return tag;
    }

    /**
     * Busca dentro do JSON de release o primeiro asset cujo "name" começa com "StudyLab" e termina com ".exe"
     * e retorna o browser_download_url correspondente.
     */
    public static String getInstallerUrl(String apiUrl) throws Exception {
        String json = httpGet(apiUrl);

        // localiza blocos "assets": [ ... ]
        Pattern assetsBlock = Pattern.compile("\"assets\"\\s*:\\s*\\[(.*?)\\]\\s*,", Pattern.DOTALL);
        Matcher m = assetsBlock.matcher(json);
        String block = null;
        if (m.find()) block = m.group(1);
        else block = json; // fallback: pesquisar em todo o JSON

        // Procura por pares "name": "StudyLab-1.0.0.exe" e pega o browser_download_url ao lado
        Pattern assetPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
        Matcher am = assetPattern.matcher(block);
        while (am.find()) {
            String assetJson = am.group(1);
            String name = findJsonStringValue("{" + assetJson + "}", "\"name\"\\s*:\\s*\"([^\"]+)\"");
            if (name != null && name.startsWith("StudyLab") && name.endsWith(".exe")) {
                String url = findJsonStringValue("{" + assetJson + "}", "\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"");
                if (url != null) return url;
            }
        }

        // fallback: tentar pegar qualquer .exe
        String anyExe = findJsonStringValue(json, "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.exe)\"");
        return anyExe;
    }

    /**
     * Compara versões simples no formato vMAJOR.MINOR.PATCH (string compare seguro para versões com mesmos dígitos).
     * Ex: isNewerVersion("v1.0.1","v1.0.0") -> true
     */
    public static boolean isNewerVersion(String latest, String current) {
        if (latest == null || current == null) return false;
        String a = latest.startsWith("v") ? latest.substring(1) : latest;
        String b = current.startsWith("v") ? current.substring(1) : current;

        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int ia = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int ib = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (ia > ib) return true;
            if (ia < ib) return false;
        }
        return false;
    }

    // --------------------- Helpers ---------------------

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("\\D.*","")); }
        catch (Exception e) { return 0; }
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestProperty("User-Agent", "StudyLab-Updater");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        try (InputStream in = connection.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }

    private static String findJsonStringValue(String json, String regex) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }
}
