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
        tasklist /fi "PID eq !OLD_PID!" | findstr /r /c:" !OLD_PID! " >nul
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
  "$jar=$env:JAR_FILE; $log=$env:LOG_FILE; $err=$env:ERR_FILE; $pidFile=$env:PID_FILE; $p=Start-Process -FilePath 'java' -ArgumentList @('-jar',$jar) -RedirectStandardOutput $log -RedirectStandardError $err -PassThru; $p.Id | Set-Content -Path $pidFile -Encoding ascii"

if errorlevel 1 (
    echo [ERROR] Startup failed.
    exit /b 1
)

if not exist "%PID_FILE%" (
    echo [ERROR] Startup failed. PID file missing.
    exit /b 1
)

set /p NEW_PID=<"%PID_FILE%"
if "!NEW_PID!"=="" (
    echo [ERROR] Startup failed. PID is empty.
    exit /b 1
)

echo [INFO] Startup succeeded. PID=!NEW_PID!
echo [INFO] Log file: %LOG_FILE%
echo [INFO] Error log: %ERR_FILE%
exit /b 0
