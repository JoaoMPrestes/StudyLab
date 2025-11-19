package com.studylab.gui;

public class ConfiguracoesData {

    public boolean randomizarQuestoes = false;
    public boolean randomizarAlternativas = false;
    public boolean mostrarCorrecao = false;
    public boolean permitirRevisao = true;

    public boolean modoProva = false;
    public int tempoPorQuestao = 8; // default

    public boolean usarSons = false;

    // Tema selecionado (ids: dark, light, neon, ocean, matrix, dracula, solarized-dark, solarized-light, custom)
    public String temaSelecionado = "dark";

    // Cores custom (hex strings, ex: #0e0f12)
    public String customBackground = "#0e0f12";
    public String customPrimary = "#00eaff";

    // Override de cor de texto (quando o usuário quiser definir manualmente)
    public boolean customOverrideText = false;
    public String customText = "#ffffff"; // se override ativo

}
