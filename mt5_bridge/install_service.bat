@echo off
REM ============================================================================
REM EMAT AutonomIQ — NSSM Windows Service Installer
REM Runs the MT5 Bridge 24/7 in the background with auto-restart on Windows VPS
REM ============================================================================
TITLE EMAT MT5 Bridge Service Installer

echo ============================================================================
echo   EMAT MT5 Bridge — 24/7 Windows Service Setup (NSSM)
echo ============================================================================

cd /d "%~dp0"

REM Check for Administrator Privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] This installer must be run as Administrator!
    echo Right-click this script and select "Run as administrator".
    pause
    exit /b 1
)

set SERVICE_NAME=EmatMt5Bridge
set SCRIPT_DIR=%~dp0
set PYTHON_EXE=%SCRIPT_DIR%venv\Scripts\python.exe

if not exist "%PYTHON_EXE%" (
    echo [ERROR] Virtual environment not found at %PYTHON_EXE%
    echo Please run start_bridge.bat once first to set up the environment.
    pause
    exit /b 1
)

REM Check for nssm.exe in current dir or PATH
where nssm >nul 2>&1
if %errorLevel% neq 0 (
    if not exist "%SCRIPT_DIR%nssm.exe" (
        echo [INFO] NSSM (Non-Sucking Service Manager) not found.
        echo Please download NSSM from https://nssm.cc/download
        echo Place nssm.exe into this directory (%SCRIPT_DIR%) and run this script again.
        pause
        exit /b 1
    )
    set NSSM_CMD="%SCRIPT_DIR%nssm.exe"
) else (
    set NSSM_CMD=nssm
)

echo [INFO] Installing Windows Service "%SERVICE_NAME%"...
%NSSM_CMD% stop %SERVICE_NAME% >nul 2>&1
%NSSM_CMD% remove %SERVICE_NAME% confirm >nul 2>&1

%NSSM_CMD% install %SERVICE_NAME% "%PYTHON_EXE%" "-m uvicorn server:app --host 0.0.0.0 --port 8080"
%NSSM_CMD% set %SERVICE_NAME% AppDirectory "%SCRIPT_DIR%"
%NSSM_CMD% set %SERVICE_NAME% Description "EMAT AutonomIQ MT5 Real-Market-Data Bridge Service"
%NSSM_CMD% set %SERVICE_NAME% Start SERVICE_AUTO_START
%NSSM_CMD% set %SERVICE_NAME% AppStdout "%SCRIPT_DIR%bridge_service.log"
%NSSM_CMD% set %SERVICE_NAME% AppStderr "%SCRIPT_DIR%bridge_service.err.log"

echo [INFO] Starting service...
%NSSM_CMD% start %SERVICE_NAME%

echo ============================================================================
echo SUCCESS: Service "%SERVICE_NAME%" is installed and running!
echo It will now run continuously in the background and restart automatically.
echo Logs are written to: %SCRIPT_DIR%bridge_service.log
echo ============================================================================
pause
