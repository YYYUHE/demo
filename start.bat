@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

set "PID_FILE=%ROOT_DIR%target\demo.pid"
set "LOG_FILE=%ROOT_DIR%target\demo.log"
set "ERR_FILE=%ROOT_DIR%target\demo.err.log"
set "JAR_FILE=%ROOT_DIR%target\demo-0.0.1-SNAPSHOT.jar"

if exist "%PID_FILE%" (
    set /p OLD_PID=<"%PID_FILE%"
    if "!OLD_PID!"=="" (
        del /f /q "%PID_FILE%" >nul 2>nul
    ) else (
        tasklist /fi "PID eq !OLD_PID!" 2>nul | findstr /r /c:" !OLD_PID! " >nul
        if !errorlevel! == 0 (
            echo [INFO] App already running. PID=!OLD_PID!
            exit /b 0
        ) else (
            del /f /q "%PID_FILE%" >nul 2>nul
        )
    )
    if exist "%PID_FILE%" (
        del /f /q "%PID_FILE%" >nul 2>nul
    )
)

echo [INFO] Building project...
call "%ROOT_DIR%mvnw.cmd" clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Build failed. Startup aborted.
    exit /b 1
)

if not exist "%JAR_FILE%" (
    echo [ERROR] Executable JAR not found: %JAR_FILE%
    exit /b 1
)

echo [INFO] Starting app in background...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Start-Process -FilePath 'javaw' -ArgumentList @('-jar',$env:JAR_FILE) -WindowStyle Hidden"

echo [INFO] Waiting for app to start...
ping -n 8 127.0.0.1 >nul 2>nul

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 .*LISTENING"') do (
    set "NEW_PID=%%a"
    goto :found_pid
)
:found_pid

if "!NEW_PID!"=="" (
    echo [WARN] Could not detect PID from port 8080. App may still be starting.
    exit /b 0
)

echo !NEW_PID!> "%PID_FILE%"

echo [INFO] Startup succeeded. PID=!NEW_PID!
echo [INFO] Log file: %LOG_FILE%
echo [INFO] Error log: %ERR_FILE%
exit /b 0
