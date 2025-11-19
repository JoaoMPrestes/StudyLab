# 🎓 StudyLab — Intelligent Quiz & Study Tool

![version](https://img.shields.io/github/v/tag/JoaoMPrestes/StudyLab?label=version&sort=semver)
![maven build](https://img.shields.io/github/actions/workflow/status/JoaoMPrestes/StudyLab/build.yml?label=Maven%20Build)

StudyLab is a modern JavaFX application designed to help students prepare for exams using dynamic quizzes, customizable themes, precision timing, error review mode, and an integrated license-control system to ensure responsible sharing.

---

## 🚀 Features

### 🧠 **Smart Quiz Engine**
- Reads formatted question files (`UTF-8`)
- Supports variable-length alternatives
- Automatic question + alternative shuffling
- Timer with countdown bar per question
- Global chronometer
- Review mode for incorrect answers

### 🎨 **Dynamic Themes System**
- Built-in themes: Dark, Light, Dracula, Neon, Ocean, Matrix, Solarized…
- Real-time preview
- Custom color theme with automatic contrast detection
- Global UI styling and consistent CSS architecture

### 🔒 **LicenseManager (Anti-sharing system)**
Prevents uncontrolled distribution by using:

- Device-ID–based authentication  
- Limited simultaneous activations  
- Automatic activation & release on program start/exit  
- GitHub-hosted license file (`licenses.json`)  
- Automatic GitHub API updates  
- Optional email notifications to admin  
- Request-access workflow  
- Zero manual user intervention

---

## 📁 Project Structure
src/
└── gui/
    ├── QuizFX.java
    ├── SplashScreen.java
    ├── Configuracoes.java
    ├── ConfiguracoesData.java
    ├── LicenseManager.java
    ├── style.css
    ├── styles/
    │   └── style-base.css
    └── themes/
        ├── dark.css
        ├── light.css
        ├── dracula.css
        ├── neon.css
        ├── ocean.css
        ├── matrix.css
        ├── solarized-dark.css
        ├── solarized-light.css
        └── custom.css

---

## 🧩 **License System (How it works)**

StudyLab contains a built-in **LicenseManager** that automatically controls usage.

### ✔ Activation flow:
1. Program generates a stable Machine ID.
2. It fetches `licenses.json` from GitHub.
3. If `active.length < maxLicenses`, activation succeeds.
4. The machine ID is added to `active[]`.
5. The updated file is committed back to GitHub automatically.
6. (Optional) An email is sent to the admin notifying activation.

### ✔ Deactivation flow
On exit:
- The machine ID is removed from `active[]`.
- GitHub receives an updated commit.
- Admin receives email (optional).

### JSON file example:


```json
{
  "maxLicenses": 100,
  "active": []
}

```

###  🖥️ Requirements
Java

☕ Java
- JDK 21 recommended (required for JPackage/JLink)

🎬 JavaFX Modules
- javafx.controls
- javafx.fxml
- javafx.graphics
- javafx.base

##  Run configuration (Eclipse/IntelliJ) must include:

```bash
--module-path PATH_TO_FX
--add-modules javafx.controls,javafx.fxml
```

##  📦 Running the Project (CLI)
🏗 Compile

```bash
javac --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml -d out src/gui/*.java
```

▶ Run

```bash
java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml gui.QuizFX
```

🛠️ Building with Maven

🏗 Compile
mvn clean compile

▶ Run (using JavaFX)
mvn javafx:run

⚙ Create Custom Runtime (JLink)
mvn jlink:jlink

📦 Create Windows Installer (.exe)
mvn jpackage:jpackage

✔ The installer will be generated inside:
target/jpackage/


##  📜 Adding Questions

Questions follow this format:
QUESTION | ALTERNATIVE A | ALTERNATIVE B | ALTERNATIVE C | CORRECT_LETTER

Example:
What is the capital of France? | Paris | Berlin | Rome | A


💡 Breaklines (\n) inside questions are supported.



##  🔧 Environment Variables (Admin only)

To enable GitHub commits & email notifications:

| Variable | Purpose |
| :--- | :--- |
| **GITHUB_TOKEN** | Token with repo scope |
| **GIT_REPO_OWNER** | Your GitHub username |
| **GIT_REPO_NAME** | Repository name |
| **SMTP_USER** | Gmail/SMTP username |
| **SMTP_PASS** | App password |
| **NOTIFY_EMAIL_TO** | Email that receives notifications |


##  📬 Activation Email Example
StudyLab activation granted
Machine ID: A1B2-C3D4-E5F6
Timestamp: 2025-11-18 14:02

##  📷 Screenshots (add after running)
add later xd

##   ⚖️ License

For educational use only.
Do not redistribute without authorization.

##  🙌 Contribution

Pull requests are welcome for:

✅ New themes

✅ Bug fixes

✅ Improvements to UI components

✅ Documentation enhancements

##  💡 About

StudyLab was designed to help students prepare for exams responsibly, combining automation, security, and modern UI with a flexible architecture for future extensions.


