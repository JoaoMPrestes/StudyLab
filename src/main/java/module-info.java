module com.studylab {

    // JavaFX
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;

    // Java AWT Desktop
    requires java.desktop;

    // Jakarta Mail (Angus)
    requires jakarta.mail;
    requires jakarta.activation;

    // SLF4J moderno (nome correto do módulo!)
    requires org.slf4j;

    // Abrir pacotes para JavaFX
    opens com.studylab.gui to javafx.fxml;

    // Exportar seu pacote principal
    exports com.studylab.gui;

    // 🔥 Necessário para o AutoUpdater funcionar
    exports com.studylab.updater;      // permite que outras partes do app acessem as classes

    // (não é necessário abrir via opens, pois não usamos FXML ou reflexão)
}
