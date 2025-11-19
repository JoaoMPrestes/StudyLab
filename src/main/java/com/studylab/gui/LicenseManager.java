package com.studylab.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class LicenseManager {

    private final String owner;
    private final String repo;
    private final String branch;
    private final String path;
    private final String token;

    private final String notifyTo;
    private final String smtpUser;
    private final String smtpPass;
    private final String smtpHost;
    private final int smtpPort;

    private final String localIdFile;
    private String localId;

    private Map<String,Object> lastJsonCache = null;

    public LicenseManager() {

        this.owner  = System.getenv().getOrDefault("GITHUB_OWNER", "");
        this.repo   = System.getenv().getOrDefault("GITHUB_REPO", "");
        this.branch = System.getenv().getOrDefault("GITHUB_BRANCH", "main");
        this.path   = System.getenv().getOrDefault("GITHUB_PATH", "licenses.json");
        this.token  = System.getenv().get("GITHUB_TOKEN");

        this.notifyTo = System.getenv().get("NOTIFY_EMAIL_TO");
        this.smtpUser = System.getenv().get("SMTP_USER");
        this.smtpPass = System.getenv().get("SMTP_PASS");
        this.smtpHost = System.getenv().getOrDefault("SMTP_HOST", "smtp.gmail.com");
        this.smtpPort = Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT", "587"));

        this.localIdFile = System.getProperty("user.home") + File.separator + ".study_lab_id";
        ensureLocalId();

        System.out.println("───────────────────────────────────────────────");
        System.out.println("[License] INIT");
        System.out.println("Owner:    " + owner);
        System.out.println("Repo:     " + repo);
        System.out.println("Branch:   " + branch);
        System.out.println("Path:     " + path);
        System.out.println("Local ID: " + localId);
        System.out.println("───────────────────────────────────────────────");
    }

    private void ensureLocalId() {
        try {
            File f = new File(localIdFile);
            if (f.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                    localId = r.readLine();
                }
            }
            if (localId == null || localId.trim().isEmpty()) {
                localId = UUID.randomUUID().toString();
                try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
                    w.write(localId);
                    w.flush();
                }
            }
        } catch (Exception e) {
            localId = UUID.randomUUID().toString();
        }
    }

    public String getLocalId() { return localId; }


    // -------------------------------------------------------
    // FETCH JSON — (com logs detalhados)
    // -------------------------------------------------------
    public String fetchRawJson() {
        try {
            String rawUrl;

            if (owner.isEmpty() || repo.isEmpty()) {
                if (path.startsWith("http")) {
                    rawUrl = path;
                } else {
                    System.out.println("[License] FETCH — No GitHub repo configured");
                    return null;
                }
            } else {
                rawUrl = String.format(
                        "https://raw.githubusercontent.com/%s/%s/%s/%s",
                        owner, repo, branch, path
                );
            }

            System.out.println("[License] FETCH — GET " + rawUrl);

            String json = httpGet(rawUrl, null);

            if (json == null) {
                System.out.println("[License] FETCH — ERROR: null response");
            } else {
                System.out.println("[License] FETCH — OK (" + json.length() + " bytes)");
            }

            return json;

        } catch (Exception ex) {
            System.out.println("[License] FETCH — EXCEPTION: " + ex.getMessage());
            return null;
        }
    }


    public Map<String,Object> parseJson(String raw) {
        Map<String,Object> out = new HashMap<>();
        if (raw == null) return out;

        try {
            Pattern pMax = Pattern.compile("\"maxLicenses\"\\s*:\\s*(\\d+)");
            Matcher mMax = pMax.matcher(raw);
            int max = 0;
            if (mMax.find()) max = Integer.parseInt(mMax.group(1));
            out.put("maxLicenses", max);

            Pattern pActive = Pattern.compile("\"active\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher mAct = pActive.matcher(raw);
            List<String> active = new ArrayList<>();

            if (mAct.find()) {
                String arr = mAct.group(1);
                Matcher mStr = Pattern.compile("\"([^\"]+)\"").matcher(arr);
                while (mStr.find()) active.add(mStr.group(1));
            }

            out.put("active", active);

        } catch (Exception e) {
            System.err.println("[License] PARSER — Error: " + e.getMessage());
        }

        return out;
    }


    // -------------------------------------------------------
    // ACQUIRE LICENSE — com logs profissionais
    // -------------------------------------------------------
    public boolean acquireLicense() {

        System.out.println("[License] ACQUIRE — Attempt for ID=" + localId);

        String raw = fetchRawJson();
        Map<String,Object> data = parseJson(raw);

        int max = (Integer) data.getOrDefault("maxLicenses", 0);
        List<String> active = (List<String>) data.getOrDefault("active", new ArrayList<>());

        System.out.println("[License] ACQUIRE — Active=" + active.size() + " / Max=" + max);

        if (active.contains(localId)) {
            System.out.println("[License] ACQUIRE — Already active");
            sendNotification("Already active", "Existing active ID: " + localId);
            return true;
        }

        if (active.size() < max) {
            System.out.println("[License] ACQUIRE — License available, activating...");
            active.add(localId);

            if (token != null && !token.isEmpty() && !owner.isEmpty()) {
                updateRemoteJson(max, active, "Activate " + localId);
                sendNotification("Activated", "ID: " + localId);
            } else {
                sendNotification("Activated (local)", "ID: " + localId + " — NOT persisted!");
            }
            return true;
        }

        System.out.println("[License] ACQUIRE — DENIED (no slots)");
        sendNotification("Denied", "No licenses available for: " + localId);
        return false;
    }


    // -------------------------------------------------------
    // RELEASE LICENSE — com logs
    // -------------------------------------------------------
    public boolean releaseLicense() {

        System.out.println("[License] RELEASE — Attempt for ID=" + localId);

        String raw = fetchRawJson();
        Map<String,Object> data = parseJson(raw);

        int max = (Integer) data.getOrDefault("maxLicenses", 0);
        List<String> active = (List<String>) data.getOrDefault("active", new ArrayList<>());

        if (!active.remove(localId)) {
            System.out.println("[License] RELEASE — ID not found, nothing to remove");
            sendNotification("Release (not found)", "ID: " + localId);
            return true;
        }

        System.out.println("[License] RELEASE — Removing from active list…");

        if (token != null && !token.isEmpty()) {
            updateRemoteJson(max, active, "Release " + localId);
            sendNotification("Released", "ID: " + localId);
        } else {
            sendNotification("Released (local)", "ID: " + localId + " — NOT persisted!");
        }

        return true;
    }


    // -------------------------------------------------------
    // UPDATE JSON — PUT no GitHub com logs detalhados
    // -------------------------------------------------------
    private boolean updateRemoteJson(int max, List<String> active, String commitMessage) {

        System.out.println("[License] UPDATE — Preparing PUT to GitHub…");

        try {
            String apiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/contents/%s",
                    owner, repo, URLEncoder.encode(path, "UTF-8")
            );

            System.out.println("[License] UPDATE — GET file info…");

            String fileInfo = httpGet(apiUrl, token);
            if (fileInfo == null) {
                System.out.println("[License] UPDATE — ERROR: GET returned null");
                return false;
            }

            Matcher mSha = Pattern.compile("\"sha\"\\s*:\\s*\"([^\"]+)\"").matcher(fileInfo);
            String sha = mSha.find() ? mSha.group(1) : null;

            System.out.println("[License] UPDATE — SHA=" + sha);

            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"maxLicenses\": ").append(max).append(",\n  \"active\": [");

            for (int i = 0; i < active.size(); i++) {
                sb.append("\"").append(active.get(i)).append("\"");
                if (i < active.size() - 1) sb.append(", ");
            }
            sb.append("]\n}");

            String json = sb.toString();
            String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            String body = "{"
                    + "\"message\":\"" + commitMessage + "\","
                    + "\"content\":\"" + base64 + "\","
                    + "\"sha\":\"" + sha + "\""
                    + "}";

            System.out.println("[License] UPDATE — Sending PUT…");

            String response = httpPut(apiUrl, body, token);

            boolean ok = response != null && response.contains("\"content\"");
            System.out.println("[License] UPDATE — Result: " + (ok ? "SUCCESS" : "FAIL"));

            return ok;

        } catch (Exception ex) {
            System.out.println("[License] UPDATE — EXCEPTION: " + ex.getMessage());
            return false;
        }
    }


    // -------------------------------------------------------
    // HTTP GET (com logs quando há erro)
    // -------------------------------------------------------
    private String httpGet(String urlStr, String token) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);

        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "token " + token);
        }

        InputStream is =
                conn.getResponseCode() < 300 ?
                        conn.getInputStream() :
                        conn.getErrorStream();

        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = r.readLine()) != null) sb.append(line).append("\n");

        return sb.toString();
    }


    // -------------------------------------------------------
    // HTTP PUT (com logs automáticos pelo updateRemoteJson)
    // -------------------------------------------------------
    private String httpPut(String urlStr, String json, String token) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "token " + token);
        }

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes(StandardCharsets.UTF_8));
        os.flush();

        InputStream is =
                conn.getResponseCode() < 300 ?
                        conn.getInputStream() :
                        conn.getErrorStream();

        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = r.readLine()) != null) sb.append(line).append("\n");

        return sb.toString();
    }


    // -------------------------------------------------------
    // EMAIL — logs profissionais
    // -------------------------------------------------------
    private void sendNotification(String subject, String body) {

        if (notifyTo == null || notifyTo.isEmpty()) {
            System.out.println("[LicenseManager] Notification disabled — no NOTIFY_EMAIL_TO set.");
            return;
        }

        if (smtpUser == null || smtpPass == null) {
            System.out.println("[LicenseManager] SMTP disabled. Notification:");
            System.out.println("To: " + notifyTo);
            System.out.println("Subject: " + subject);
            System.out.println(body);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));

        // Gmail mandatory settings (fix EOF)
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.smtp.allow8bitmime", "true");
        props.put("mail.smtp.sendpartial", "true");

        // Fix for EOF — force proper EHLO behavior
        props.put("mail.smtp.localhost", "localhost");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPass);
            }
        });

        try {

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(smtpUser));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(notifyTo));
            msg.setSubject("StudyLab License — " + subject);

            msg.setText(
                    "Timestamp: " + Instant.now() + "\n\n"
                    + body + "\n\n"
                    + "Machine ID: " + localId
            );

            Transport.send(msg);
            System.out.println("[License] EMAIL — Sent successfully.");

        } catch (MessagingException e) {
            System.err.println("[LicenseManager] Email error: " + e.getMessage());
        }
    }


}
