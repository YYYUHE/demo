@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

set "PID_FILE=%ROOT_DIR%target\demo.pid"

if not exist "%PID_FILE%" (
    echo [INFO] PID file not found. App may not be running.
    exit /b 0
)

set /p PID=<"%PID_FILE%"
if "%PID%"=="" (
    echo [WARN] PID file is empty. Cleaning it up...
    del /f /q "%PID_FILE%" >nul 2>nul
    exit /b 0
)

tasklist /fi "PID eq %PID%" | findstr /r /c:" %PID% " >nul
if errorlevel 1 (
    echo [INFO] Process not found. Removing PID file.
    del /f /q "%PID_FILE%" >nul 2>nul
    exit /b 0
)

echo [INFO] Stopping app. PID=%PID%...
taskkill /pid %PID% /f >nul
if errorlevel 1 (
    echo [ERROR] Stop failed. Please check process manually.
    exit /b 1
)

del /f /q "%PID_FILE%" >nul 2>nul
echo [INFO] App stopped.
exit /b 0
