@echo off
setlocal EnableDelayedExpansion

echo [StudyLab] Detectando ambiente...

set JAR=StudyLab.jar
set BASE_DIR=%~dp0..

if exist "%BASE_DIR%\%JAR%" (
    set JAR_PATH="%BASE_DIR%\%JAR%"
) else (
    echo [ERRO] Arquivo %JAR% não encontrado!
    pause
    exit /b 1
)

if exist "%BASE_DIR%\runtime\bin\java.exe" (
    set JAVA_CMD="%BASE_DIR%\runtime\bin\java.exe"
    echo [StudyLab] Usando Java empacotado.
) else (
    echo [StudyLab] Usando Java do sistema.
    set JAVA_CMD=java
)

echo [StudyLab] Iniciando...
%JAVA_CMD% -jar %JAR_PATH%
endlocal