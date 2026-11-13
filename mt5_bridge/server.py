"""
EMAT AutonomIQ — Production MT5 Python Bridge Server
Provides REST and WebSocket endpoints for MT5 market data connectivity.
Read-Only: No trade execution or order placement logic exists.
"""
import asyncio
import json
import logging
import os
import sys
import time
from typing import Any, Dict, List, Optional

from auth import verify_token
from config import BridgeConfig
from mt5_manager import MT5Manager

logger = logging.getLogger("EMAT.BridgeServer")
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")

# Global MT5 Manager singleton
manager = MT5Manager()

# Try importing FastAPI
FASTAPI_AVAILABLE = False
try:
    from fastapi import FastAPI, Header, HTTPException, Query, WebSocket, WebSocketDisconnect, status
    from fastapi.middleware.cors import CORSMiddleware
    from fastapi.responses import JSONResponse
    from pydantic import BaseModel
    FASTAPI_AVAILABLE = True
except ImportError:
    logger.warning("FastAPI not installed in current environment. Using standard HTTP fallback if run directly.")

if FASTAPI_AVAILABLE:
    app = FastAPI(
        title="EMAT AutonomIQ MT5 Bridge",
        description="High-performance, secure read-only market data bridge between MetaTrader 5 and EMAT AutonomIQ.",
        version="1.0.4"
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    class ConnectRequest(BaseModel):
        server: str
        account: int
        investor_password: str
        path: Optional[str] = None

    def authenticate_request(authorization: Optional[str] = Header(None)):
        if not authorization or not verify_token(authorization):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Unauthorized: Invalid or missing Bridge Bearer Token"
            )

    @app.on_event("startup")
    async def startup_event():
        logger.info("Starting EMAT MT5 Bridge Server on %s:%d", BridgeConfig.HOST, BridgeConfig.PORT)
        token = BridgeConfig.get_or_create_token()
        logger.info("Bridge Authentication Token configured (Length: %d)", len(token))
        # Auto-connect if environment credentials provided
        if BridgeConfig.MT5_SERVER and BridgeConfig.MT5_LOGIN and BridgeConfig.MT5_INVESTOR_PASSWORD:
            logger.info("Auto-connecting to MT5 Server: %s (Account: %d)", BridgeConfig.MT5_SERVER, BridgeConfig.MT5_LOGIN)
            manager.connect(
                server=BridgeConfig.MT5_SERVER,
                login=BridgeConfig.MT5_LOGIN,
                password=BridgeConfig.MT5_INVESTOR_PASSWORD,
                path=BridgeConfig.MT5_TERMINAL_PATH
            )

    @app.on_event("shutdown")
    async def shutdown_event():
        logger.info("Shutting down MT5 Bridge Server")
        manager.disconnect()

    # Health Endpoints (Both root and versioned for Android Mt5BridgeClient)
    @app.get("/health")
    @app.get("/api/v1/health")
    def health_check(authorization: Optional[str] = Header(None)):
        authenticate_request(authorization)
        coverage = manager.symbol_mapper.get_coverage_summary()
        return {
            "status": "OK",
            "terminalConnected": manager.is_connected,
            "accountNumber": str(manager.account_number) if manager.account_number else "",
            "server": manager.server_name,
            "symbolsCount": coverage["mapped_count"],
            "totalCanonical": coverage["total_canonical"],
            "timestamp": int(time.time() * 1000),
            "version": "1.0.4",
            "readOnly": True
        }

    @app.get("/connection/status")
    def connection_status(authorization: Optional[str] = Header(None)):
        authenticate_request(authorization)
        coverage = manager.symbol_mapper.get_coverage_summary()
        acc_info = manager.get_account_info()
        return {
            "connected": manager.is_connected,
            "terminalConnected": manager.is_connected,
            "server": manager.server_name,
            "accountNumber": manager.account_number,
            "accountType": acc_info.get("account_type", "DEMO"),
            "company": manager.broker_company,
            "lastConnectTime": manager.last_connect_time,
            "lastError": manager.last_error_message,
            "readOnly": True,
            "tradeAllowed": False,
            "coverage": coverage
        }

    @app.post("/connection/connect")
    def connect_mt5(req: ConnectRequest, authorization: Optional[str] = Header(None)):
        authenticate_request(authorization)
        if not req.server.strip():
            raise HTTPException(status_code=400, detail="Broker server name is required")
        if req.account <= 0:
            raise HTTPException(status_code=400, detail="Valid MT5 account number is required")
        if not req.investor_password.strip():
            raise HTTPException(status_code=400, detail="Investor password is required")

        success, msg = manager.connect(
            server=req.server,
            login=req.account,
            password=req.investor_password,
            path=req.path
        )
        if not success:
            raise HTTPException(status_code=502, detail=f"MT5 Connection Failed: {msg}")

        return {
            "success": True,
            "message": msg,
            "accountNumber": manager.account_number,
            "server": manager.server_name,
            "discoveredSymbols": manager.symbol_mapper.get_coverage_summary()
        }

    @app.post("/connection/disconnect")
    def disconnect_mt5(authorization: Optional[str] = Header(None)):
        authenticate_request(authorization)
        manager.disconnect()
        return {"success": True, "message": "Disconnected from MT5"}

    @app.get("/account/info")
    def account_info(authorization: Optional[str] = Header(None)):
        authenticate_request(authorization)
        return manager.get_account_info()

    @app.get("/symbols")
    def get_symbols(authorization: Optional[str] = Header(None)):
        authenticate_request(authorization)
        return manager.symbol_mapper.get_coverage_summary()

    # Quotes Endpoints (Root, /market/quotes, /api/v1/quotes)
    @app.get("/market/quotes")
    @app.get("/api/v1/quotes")
    @app.get("/quotes")
    def get_quotes(authorization: Optional[str] = Header(None)):
        authenticate_request(authorization)
        quotes = manager.get_quotes()
        return {
            "quotes": quotes,
            "count": len(quotes),
            "timestamp": int(time.time() * 1000)
        }

    @app.get("/market/quote/{symbol}")
    def get_single_quote(symbol: str, authorization: Optional[str] = Header(None)):
        authenticate_request(authorization)
        quote = manager.get_single_quote(symbol)
        if not quote:
            raise HTTPException(status_code=404, detail=f"Quote not found for symbol: {symbol}")
        return quote

    @app.get("/market/ohlc/{symbol}")
    def get_ohlc(
        symbol: str,
        timeframe: str = Query("H1", description="Candle timeframe: M1, M5, M15, M30, H1, H4, D1"),
        count: int = Query(100, ge=1, le=1000, description="Number of candles"),
        authorization: Optional[str] = Header(None)
    ):
        authenticate_request(authorization)
        candles = manager.get_ohlc(symbol=symbol, timeframe=timeframe, count=count)
        return {
            "symbol": symbol.upper(),
            "timeframe": timeframe.upper(),
            "count": len(candles),
            "candles": candles
        }

    # WebSocket Real-Time Stream
    @app.websocket("/market/stream")
    async def websocket_stream(websocket: WebSocket, token: Optional[str] = Query(None)):
        # Authenticate token from query param or header
        auth_header = websocket.headers.get("authorization")
        if not verify_token(auth_header) and not verify_token(token):
            await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
            return

        await websocket.accept()
        logger.info("WebSocket client connected to /market/stream")
        try:
            while True:
                quotes = manager.get_quotes()
                payload = {
                    "type": "TICK_UPDATE",
                    "timestamp": int(time.time() * 1000),
                    "quotes": quotes
                }
                await websocket.send_text(json.dumps(payload))
                await asyncio.sleep(BridgeConfig.POLL_INTERVAL_SEC)
        except WebSocketDisconnect:
            logger.info("WebSocket client disconnected")
        except Exception as e:
            logger.error("WebSocket streaming exception: %s", e)


# Lightweight Standalone Runner using Python standard library http.server
# This guarantees that the bridge can run or be tested even before pip dependencies are installed!
def run_standalone_server(port: int = 8080):
    from http.server import HTTPServer, BaseHTTPRequestHandler
    from urllib.parse import urlparse, parse_qs
    import ssl

    class StandaloneHandler(BaseHTTPRequestHandler):
        def log_message(self, format, *args):
            logger.info("%s - %s", self.address_string(), format % args)

        def _send_json(self, status_code: int, data: Any):
            body = json.dumps(data).encode("utf-8")
            self.send_response(status_code)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def do_OPTIONS(self):
            self.send_response(204)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            self.end_headers()

        def do_GET(self):
            parsed = urlparse(self.path)
            path = parsed.path
            params = parse_qs(parsed.query)

            # Auth check
            auth_header = self.headers.get("Authorization")
            token_param = params.get("token", [None])[0]
            if not verify_token(auth_header) and not verify_token(token_param):
                self._send_json(401, {"error": "Unauthorized", "detail": "Missing or invalid Bearer token"})
                return

            if path in ("/health", "/api/v1/health"):
                coverage = manager.symbol_mapper.get_coverage_summary()
                self._send_json(200, {
                    "status": "OK",
                    "terminalConnected": manager.is_connected,
                    "accountNumber": str(manager.account_number) if manager.account_number else "",
                    "server": manager.server_name,
                    "symbolsCount": coverage["mapped_count"],
                    "totalCanonical": coverage["total_canonical"],
                    "timestamp": int(time.time() * 1000),
                    "version": "1.0.4",
                    "readOnly": True
                })
            elif path == "/connection/status":
                coverage = manager.symbol_mapper.get_coverage_summary()
                acc_info = manager.get_account_info()
                self._send_json(200, {
                    "connected": manager.is_connected,
                    "terminalConnected": manager.is_connected,
                    "server": manager.server_name,
                    "accountNumber": manager.account_number,
                    "accountType": acc_info.get("account_type", "DEMO"),
                    "company": manager.broker_company,
                    "lastConnectTime": manager.last_connect_time,
                    "lastError": manager.last_error_message,
                    "readOnly": True,
                    "tradeAllowed": False,
                    "coverage": coverage
                })
            elif path == "/account/info":
                self._send_json(200, manager.get_account_info())
            elif path == "/symbols":
                self._send_json(200, manager.symbol_mapper.get_coverage_summary())
            elif path in ("/market/quotes", "/api/v1/quotes", "/quotes"):
                quotes = manager.get_quotes()
                self._send_json(200, {
                    "quotes": quotes,
                    "count": len(quotes),
                    "timestamp": int(time.time() * 1000)
                })
            elif path.startswith("/market/quote/"):
                sym = path.split("/")[-1]
                q = manager.get_single_quote(sym)
                if q:
                    self._send_json(200, q)
                else:
                    self._send_json(404, {"error": f"Symbol not found: {sym}"})
            elif path.startswith("/market/ohlc/"):
                sym = path.split("/")[-1]
                tf = params.get("timeframe", ["H1"])[0]
                cnt = int(params.get("count", ["100"])[0])
                candles = manager.get_ohlc(sym, tf, cnt)
                self._send_json(200, {
                    "symbol": sym.upper(),
                    "timeframe": tf.upper(),
                    "count": len(candles),
                    "candles": candles
                })
            else:
                self._send_json(404, {"error": "Not Found"})

        def do_POST(self):
            parsed = urlparse(self.path)
            path = parsed.path

            auth_header = self.headers.get("Authorization")
            if not verify_token(auth_header):
                self._send_json(401, {"error": "Unauthorized"})
                return

            length = int(self.headers.get("Content-Length", 0))
            body_data = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
            try:
                body_json = json.loads(body_data)
            except Exception:
                body_json = {}

            if path == "/connection/connect":
                srv = body_json.get("server", "")
                acc = int(body_json.get("account", 0))
                pw = body_json.get("investor_password", "")
                success, msg = manager.connect(server=srv, login=acc, password=pw)
                if success:
                    self._send_json(200, {
                        "success": True,
                        "message": msg,
                        "accountNumber": manager.account_number,
                        "server": manager.server_name
                    })
                else:
                    self._send_json(502, {"error": msg})
            elif path == "/connection/disconnect":
                manager.disconnect()
                self._send_json(200, {"success": True, "message": "Disconnected from MT5"})
            else:
                self._send_json(404, {"error": "Not Found"})

    token = BridgeConfig.get_or_create_token()
    print("=" * 64)
    print(" EMAT AutonomIQ — MT5 Python Bridge Server")
    print(f" Port: {port} | Host: {BridgeConfig.HOST}")
    print(f" API Bearer Token: {token}")
    print("=" * 64)
    
    server_address = (BridgeConfig.HOST, port)
    httpd = HTTPServer(server_address, StandaloneHandler)
    
    if BridgeConfig.SSL_CERT_FILE and BridgeConfig.SSL_KEY_FILE:
        if os.path.exists(BridgeConfig.SSL_CERT_FILE) and os.path.exists(BridgeConfig.SSL_KEY_FILE):
            ctx = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
            ctx.load_cert_chain(certfile=BridgeConfig.SSL_CERT_FILE, keyfile=BridgeConfig.SSL_KEY_FILE)
            httpd.socket = ctx.wrap_socket(httpd.socket, server_side=True)
            logger.info("SSL/TLS enabled using cert: %s", BridgeConfig.SSL_CERT_FILE)

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("Stopping server")
    finally:
        httpd.server_close()


if __name__ == "__main__":
    port = int(os.getenv("BRIDGE_PORT", "8080"))
    if FASTAPI_AVAILABLE:
        import uvicorn
        token = BridgeConfig.get_or_create_token()
        print("=" * 64)
        print(" EMAT AutonomIQ — Production MT5 Python Bridge (FastAPI/Uvicorn)")
        print(f" Port: {port} | Host: {BridgeConfig.HOST}")
        print(f" API Bearer Token: {token}")
        print("=" * 64)
        uvicorn.run("server:app", host=BridgeConfig.HOST, port=port, reload=False)
    else:
        run_standalone_server(port=port)
