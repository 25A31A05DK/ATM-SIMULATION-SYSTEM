# ATM Simulation System - PowerShell Runner
Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "       ATM SIMULATION SYSTEM - STARTING BACKEND          " -ForegroundColor Yellow
Write-Host "=========================================================" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $scriptDir

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "Compiling Java source files..." -ForegroundColor Green
$files = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
javac -d "bin" $files

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Java compilation failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Compilation successful! Starting server..." -ForegroundColor Green
Set-Location "$scriptDir\.."
java -cp "backend\bin;backend\lib\*" com.atm.AtmApplication
