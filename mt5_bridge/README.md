# EMAT AutonomIQ — MT5 Python Bridge Server

**Brick 3 — Real Market Data (Read-Only)**  
*Verifiable AI Trading Intelligence • UNIEE AI Technologies*  
*Principle: Real Data. Real Results.*

---

## 1. Architectural Overview

The MT5 Python Bridge is a lightweight, high-performance, **strictly read-only** middleware server designed to run on a **Windows VPS** or Windows computer alongside the **MetaTrader 5 (MT5)** client terminal.

```
┌──────────────────────────────────────┐
│     EMAT AutonomIQ Android App       │
│  (Settings → Data Connections → MT5) │
└──────────────────┬───────────────────┘
                   │
                   │ HTTPS / REST & WebSockets (Bearer Auth)
                   ▼
┌────────────────────────────────────────────────────────┐
│     EMAT MT5 Python Bridge Server (Windows VPS)        │
│  - FastAPI / Uvicorn ASGI Server (Port 8080)           │
│  - Bearer Token Authentication (Timing-Safe)          │
│  - Dynamic Symbol Mapper (HFM Suffixes & Aliases)      │
│  - Zero Execution Code (Trade Allowed = False)         │
└──────────────────┬─────────────────────────────────────┘
                   │
                   │ Official MetaTrader5 Python IPC (Win32)
                   ▼
┌────────────────────────────────────────────────────────┐
│    MetaTrader 5 Terminal (e.g. HF Markets - HFM)       │
│  - Logged in via Investor Password (Read-Only)         │
│  - Real-Time Tick & Depth Feed                         │
│  - Full 19 Canonical Instruments Subscribed            │
└────────────────────────────────────────────────────────┘
```

### Core Invariants Preserved
1. **Strictly Read-Only**: The bridge contains zero order placement, position modification, or trading code (`order_send` is strictly absent).
2. **Investor Authentication**: Only the read-only MT5 investor password is used.
3. **Canonical 19-Instrument Coverage**: Auto-discovers and maps broker symbols (including HFM suffixes such as `EURUSDm`, `EURUSD.pro`, `USTEC`, `USA500`, etc.) to the 19 EMAT standard instruments.
4. **Independent Validation**: Feeds live bid, ask, and spread directly into the Android EMAT comparison matrix alongside biquote.

---

## 2. Requirements

- **Operating System**: Windows 10, Windows 11, or Windows Server 2019/2022 VPS.
- **Python**: Version 3.9 or higher (Ensure *"Add Python to PATH"* is checked during installation).
- **MetaTrader 5 Terminal**: Installed (e.g. HF Markets MT5, IC Markets, Exness, or standard MetaQuotes MT5).
- **Network**: Port `8080` (or your chosen port) open in Windows Firewall.

---

## 3. Quick Start on Windows VPS

### Step 1: Copy the Bridge Folder to your VPS
Transfer the `mt5_bridge` directory to your Windows VPS (e.g. `C:\emat_mt5_bridge\`).

### Step 2: Configure MT5 Terminal & Investor Password
1. Launch your broker's MetaTrader 5 terminal on the VPS (e.g., HF Markets MT5).
2. Login to your trading account:
   - **Login**: Your MT5 account number (e.g. `12345678`).
   - **Password**: Your **Investor Password** (Read-Only).  
     *(In HFM client area, navigate to My Accounts → Change Investor Password if needed)*.
   - **Server**: Your exact broker server (e.g. `HFMarketsSV-Live01` or `HFMarketsSA-Demo`).
3. Check the bottom right corner of MT5 to ensure the connection bars are green and ping is active.

### Step 3: Launch the Bridge
Double-click `start_bridge.bat`. The script will automatically:
1. Create a Python virtual environment (`venv`).
2. Upgrade `pip` and install all dependencies from `requirements.txt`.
3. Generate a secure 256-bit authentication token in `.bridge_token` (if one does not exist).
4. Display the token and start the server on `0.0.0.0:8080`.

Example output:
```text
============================================================================
  EMAT AutonomIQ — MT5 Python Bridge Server
============================================================================
BRIDGE AUTHENTICATION TOKEN:
k9s8D_3kLpQ7vNwY1xZ4mR8tU2aC6eH0jF5bE9wG3rM=
----------------------------------------------------------------------------
Starting FastAPI/Uvicorn Server on port 8080...
INFO:     Started server process [4321]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8080
```

---

## 4. Run Continuously 24/7 as a Windows Service (NSSM)

To ensure the bridge starts automatically when the VPS boots and restarts if interrupted:

1. Download **NSSM** (Non-Sucking Service Manager) from [nssm.cc/download](https://nssm.cc/download).
2. Extract `nssm.exe` (from the `win64` folder) into the `mt5_bridge` directory.
3. Right-click `install_service.bat` and select **"Run as administrator"**.
4. The service `EmatMt5Bridge` will be installed, set to automatic startup, and started immediately.
5. Service logs are maintained in `bridge_service.log` and `bridge_service.err.log`.

---

## 5. Connecting from the EMAT Android App

1. In EMAT AutonomIQ, open the left navigation drawer.
2. Navigate to **SETTINGS → DATA CONNECTIONS → MY MT5 BROKER**.
3. Fill in your broker details:
   - **Broker Name**: `HF Markets` (or your broker)
   - **Broker Server**: e.g., `HFMarketsSV-Live01`
   - **MT5 Account Number**: e.g., `12345678`
   - **Investor Password**: Your read-only investor password
4. Fill in Bridge Configuration:
   - **MT5 Bridge URL**: `http://YOUR_VPS_IP:8080` (or your custom HTTPS domain)
   - **Bridge Authentication Token**: Paste the token displayed by `start_bridge.bat` or `.bridge_token`
5. Tap **"SAVE CONFIGURATION"**.
6. Tap **"TEST CONNECTION"**.
   - You should see: `PASS: Connected to MT5 Bridge • Account verified • 19/19 instruments active`.
7. Tap **"REFRESH BROKER QUOTES"** to populate the 19-instrument comparison matrix comparing MT5 live quotes with biquote.

---

## 6. Dynamic Symbol Discovery & HFM Mapping

The bridge dynamically discovers broker-specific symbol nomenclature and maps them to EMAT's 19 standard instruments:

| Canonical | HFM Micro / Pro / Cent | Indices / Metals Aliases |
| :--- | :--- | :--- |
| **EURUSD** | `EURUSDm`, `EURUSD.pro`, `EURUSD#` | `EURUSD` |
| **GBPUSD** | `GBPUSDm`, `GBPUSD.pro`, `GBPUSD#` | `GBPUSD` |
| **USDJPY** | `USDJPYm`, `USDJPY.pro`, `USDJPY#` | `USDJPY` |
| **USDCHF** | `USDCHFm`, `USDCHF.pro`, `USDCHF#` | `USDCHF` |
| **USDCAD** | `USDCADm`, `USDCAD.pro`, `USDCAD#` | `USDCAD` |
| **AUDUSD** | `AUDUSDm`, `AUDUSD.pro`, `AUDUSD#` | `AUDUSD` |
| **NZDUSD** | `NZDUSDm`, `NZDUSD.pro`, `NZDUSD#` | `NZDUSD` |
| **EURGBP** | `EURGBPm`, `EURGBP.pro`, `EURGBP#` | `EURGBP` |
| **EURJPY** | `EURJPYm`, `EURJPY.pro`, `EURJPY#` | `EURJPY` |
| **GBPJPY** | `GBPJPYm`, `GBPJPY.pro`, `GBPJPY#` | `GBPJPY` |
| **XAUUSD** | `XAUUSDm`, `XAUUSD.pro` | `GOLD` |
| **XAGUSD** | `XAGUSDm`, `XAGUSD.pro` | `SILVER` |
| **NAS100** | `NAS100m`, `NAS100.cash` | `USTEC`, `USA100`, `TECH100` |
| **US500** | `US500m`, `US500.cash` | `SPX500`, `USA500` |
| **US30** | `US30m`, `US30.cash` | `DJ30`, `USA30`, `WALLSTREET` |
| **GER40** | `GER40m`, `GER40.cash` | `DE40`, `DAX40` |
| **UK100** | `UK100m`, `UK100.cash` | `FTSE100`, `GB100` |
| **BTCUSD** | `BTCUSDm`, `BTCUSDT` | `BITCOIN` |
| **ETHUSD** | `ETHUSDm`, `ETHUSDT` | `ETHEREUM` |

---

## 7. REST & WebSocket API Specification

All requests require the Bearer authentication header:
`Authorization: Bearer <TOKEN>`

### 1. Health & Status
- **`GET /health`** or **`GET /api/v1/health`**
  ```json
  {
    "status": "OK",
    "terminalConnected": true,
    "accountNumber": "12345678",
    "server": "HFMarketsSV-Live01",
    "symbolsCount": 19,
    "totalCanonical": 19,
    "timestamp": 1716300000000,
    "version": "1.0.4",
    "readOnly": true
  }
  ```

- **`GET /connection/status`**
  Returns detailed bridge health, uptime, terminal state, and mapping coverage.

### 2. Live Quotes
- **`GET /market/quotes`** or **`GET /api/v1/quotes`**
  ```json
  {
    "quotes": [
      {
        "symbol": "EURUSD",
        "broker_symbol": "EURUSDm",
        "bid": 1.08502,
        "ask": 1.08518,
        "spread": 0.00016,
        "timestamp": 1716300000123,
        "source": "MT5 Broker (HFMarketsSV-Live01)"
      }
    ],
    "count": 19,
    "timestamp": 1716300000123
  }
  ```

- **`GET /market/quote/{symbol}`**
  Returns live tick for a single instrument (e.g. `GET /market/quote/XAUUSD`).

### 3. Historical Candles
- **`GET /market/ohlc/{symbol}?timeframe=H1&count=100`**
  Timeframes: `M1`, `M5`, `M15`, `M30`, `H1`, `H4`, `D1`.

### 4. WebSocket Streaming
- **`WebSocket /market/stream?token=<TOKEN>`**
  Streams JSON tick arrays continuously at 1-second intervals.

---

## 8. Windows Firewall Configuration

To allow incoming traffic on port 8080:
1. Open PowerShell as Administrator on the VPS.
2. Run:
   ```powershell
   New-NetFirewallRule -DisplayName "EMAT MT5 Bridge" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
   ```

---

## 9. Automated Testing

To run the full unit and integration test suite:
```bash
python -m unittest discover -s tests -p "test_*.py" -v
```
All 16 test cases verify auth security, symbol suffix mapping, read-only invariants, OHLC retrieval, and REST endpoints.
