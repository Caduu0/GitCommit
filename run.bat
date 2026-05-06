@echo off
title Git Commit Mirror - Bot Local
echo ===================================================
echo Iniciando o Git Commit Mirror localmente para testes
echo ===================================================
echo.
echo As credenciais do application.properties serao utilizadas.
echo.

:: Verifica se o Maven esta instalado, se não estiver, baixa automaticamente.
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    if exist ".maven\apache-maven-3.9.6\bin\mvn.cmd" (
        echo [INFO] Usando Maven local baixado na pasta .maven...
        set "PATH=%CD%\.maven\apache-maven-3.9.6\bin;%PATH%"
    ) else (
        echo [ERRO] Maven nao encontrado!
        echo Baixando Maven temporariamente para rodar o projeto...
        powershell -Command "$ProgressPreference = 'SilentlyContinue'; $url = 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip'; Invoke-WebRequest -Uri $url -OutFile 'maven.zip' -UseBasicParsing; Expand-Archive -Path 'maven.zip' -DestinationPath '.maven' -Force; Remove-Item 'maven.zip'"
        set "PATH=%CD%\.maven\apache-maven-3.9.6\bin;%PATH%"
    )
)

echo Compilando e iniciando a aplicacao com Spring Boot...
call mvn spring-boot:run

pause