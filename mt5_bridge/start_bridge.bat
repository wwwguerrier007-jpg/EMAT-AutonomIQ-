@echo off
REM ============================================================================
REM EMAT AutonomIQ — MT5 Python Bridge Startup Script (Windows VPS / Local PC)
REM ============================================================================
TITLE EMAT MT5 Bridge Server

echo ============================================================================
echo   EMAT AutonomIQ — MT5 Python Bridge Server
echo   Target Broker: XM Global (DEMO) / Any MT5 Broker
echo ============================================================================

REM Check if Python is installed
python --version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Python 3.9+ is not found in PATH!
    echo Please install Python from https://www.python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation.
    pause
    exit /b 1
)

cd /d "%~dp0"

REM Check or create virtual environment
if not exist "venv" (
    echo [INFO] Creating Python virtual environment (venv)...
    python -m venv venv
)

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Install or update dependencies
echo [INFO] Checking and installing dependencies...
python -m pip install --upgrade pip
pip install -r requirements.txt

REM Ensure token exists
if not exist ".bridge_token" (
    echo [INFO] Generating new security token...
    python generate_token.py
)

REM Display token to operator
set /p BRIDGE_TOKEN=<.bridge_token
echo ----------------------------------------------------------------------------
echo BRIDGE AUTHENTICATION TOKEN:
echo %BRIDGE_TOKEN%
echo ----------------------------------------------------------------------------
echo Copy this token into EMAT AutonomIQ Android app:
echo Settings -^> Data Connections -^> My MT5 Broker -^> Bridge Authentication Token
echo ----------------------------------------------------------------------------

REM Launch Bridge Server
echo [INFO] Starting FastAPI/Uvicorn Server on port 8080...
python -m uvicorn server:app --host 0.0.0.0 --port 8080

pause
