@echo off
title ATM SIMULATION SYSTEM - LAUNCHER
cls
echo ==================================================================
echo                  ATM SIMULATION SYSTEM
echo ==================================================================
echo  Starting Backend Server and opening Web Application...
echo ==================================================================

cd /d "%~dp0"

:: Check if compiled bin directory exists, if not compile
if not exist "backend\bin" (
    mkdir "backend\bin"
    echo [1/2] Compiling Java backend...
    javac -d "backend\bin" backend\src\main\java\com\atm\model\*.java backend\src\main\java\com\atm\config\*.java backend\src\main\java\com\atm\util\*.java backend\src\main\java\com\atm\dao\*.java backend\src\main\java\com\atm\service\*.java backend\src\main\java\com\atm\controller\*.java backend\src\main\java\com\atm\server\*.java backend\src\main\java\com\atm\AtmApplication.java
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Compilation failed. Please make sure Java JDK is installed.
        pause
        exit /b %ERRORLEVEL%
    )
)

echo [2/2] Launching ATM Backend on http://localhost:8080 ...
start "" http://localhost:8080/
java -cp "backend\bin;backend\lib\*" com.atm.AtmApplication

pause
