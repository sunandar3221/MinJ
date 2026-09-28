@echo off
title MinJ - Minecraft Java Edition
cd /d "%~dp0"
echo ==============================================
echo       MinJ - Minecraft Java Edition
echo ==============================================
echo.

if exist MinJ.jar (
    echo Memulai MinJ dari JAR...
    java -Xmx1024M -jar MinJ.jar
) else (
    echo Mengompilasi kode sumber Java...
    if not exist bin mkdir bin
    javac -d bin src\com\minj\*.java
    if errorlevel 1 (
        echo [ERROR] Kompilasi gagal! Pastikan JDK terpasang.
        pause
        exit /b 1
    )
    echo Memulai Game...
    java -Xmx1024M -cp bin com.minj.Main
)

if errorlevel 1 (
    echo.
    echo Game ditutup atau terjadi error.
    pause
)
