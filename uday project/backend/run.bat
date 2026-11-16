@echo off
title ATM Simulation System - Backend Server
echo =========================================================
echo       ATM SIMULATION SYSTEM - STARTING BACKEND
echo =========================================================

cd /d "%~dp0"
if not exist "bin" mkdir bin

echo Compiling Java source files...
javac -d bin src\main\java\com\atm\model\*.java src\main\java\com\atm\config\*.java src\main\java\com\atm\util\*.java src\main\java\com\atm\dao\*.java src\main\java\com\atm\service\*.java src\main\java\com\atm\controller\*.java src\main\java\com\atm\AtmApplication.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed! Please ensure JDK 11+ is installed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Starting ATM Backend Server on http://localhost:8080 ...
echo (Keep this window open while using the ATM system)
echo.
cd ..
java -cp "backend\bin;backend\lib\*" com.atm.AtmApplication
pause
