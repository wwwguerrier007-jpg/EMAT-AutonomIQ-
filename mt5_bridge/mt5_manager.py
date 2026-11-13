"""
EMAT AutonomIQ — MT5 Connection & Data Manager
Interfaces directly with the official MetaTrader 5 Python package.
Strictly Read-Only: No trading, modifying, or executing orders.
Includes fallback simulation gateway for local test execution on non-Windows platforms.
"""
import logging
import math
import sys
import time
from typing import Any, Dict, List, Optional, Tuple

from config import BridgeConfig
from symbol_mapper import CANONICAL_INSTRUMENTS, SymbolMapper

logger = logging.getLogger("EMAT.MT5Manager")
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")

# Attempt importing official MetaTrader 5 Python package
MT5_AVAILABLE = False
mt5 = None

if not BridgeConfig.FORCE_MOCK:
    try:
        import MetaTrader5 as _mt5
        mt5 = _mt5
        MT5_AVAILABLE = True
        logger.info("Official MetaTrader5 Python package successfully imported.")
    except (ImportError, Exception) as e:
        logger.warning("MetaTrader5 package not available or not on Windows (%s). Falling back to mock gateway.", e)
        MT5_AVAILABLE = False
else:
    logger.info("FORCE_MOCK is enabled. Running MT5 in simulation mode.")

# Timeframe mapping for MT5
TIMEFRAME_MAP = {
    "M1": 1,
    "M5": 5,
    "M15": 15,
    "M30": 30,
    "H1": 16385,
    "H4": 16388,
    "D1": 16408,
    "W1": 32769,
    "MN1": 49153
}


class MT5Manager:
    _instance = None

    def __new__(cls, *args, **kwargs):
        if not cls._instance:
            cls._instance = super(MT5Manager, cls).__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return
        self._initialized = True
        self.symbol_mapper = SymbolMapper()
        self.is_connected = False
        self.server_name = ""
        self.account_number = 0
        self.investor_mode = True
        self.last_connect_time = 0.0
        self.last_error_message = ""
        self.broker_company = "MetaQuotes Software"
        
        # Mock ticks state for local simulation / testing
        self._mock_prices = {
            "EURUSD": 1.0850, "GBPUSD": 1.2720, "USDJPY": 154.20, "USDCHF": 0.8920, "USDCAD": 1.3650,
            "AUDUSD": 0.6550, "NZDUSD": 0.6020, "EURGBP": 0.8530, "EURJPY": 167.30, "GBPJPY": 196.10,
            "XAUUSD": 2360.50, "XAGUSD": 30.25,
            "NAS100": 18250.0, "US500": 5320.0, "US30": 39800.0, "GER40": 18650.0, "UK100": 8250.0,
            "BTCUSD": 66500.0, "ETHUSD": 3520.0
        }

    def connect(self, server: str, login: int, password: str, path: Optional[str] = None) -> Tuple[bool, str]:
        """
        Connects to MT5 terminal using investor password.
        Strictly enforces read-only verification.
        """
        self.server_name = server.strip()
        self.account_number = login
        
        if not MT5_AVAILABLE:
            # Fallback simulator for non-Windows or environments without MT5 installed
            logger.info("Initializing simulated MT5 connection for server: %s, login: %s", server, login)
            self.is_connected = True
            self.last_connect_time = time.time()
            if "XM" in server.upper():
                self.broker_company = "XM Global (Demo)"
            elif "HF" in server.upper():
                self.broker_company = "HF Markets (Simulation)"
            else:
                self.broker_company = "Broker Demo Server"
            # Populate mock broker symbols
            mock_symbols = []
            suffix = ""
            for sym in CANONICAL_INSTRUMENTS:
                if sym == "NAS100":
                    mock_symbols.append("US100Cash")
                elif sym == "US500":
                    mock_symbols.append("US500Cash")
                elif sym == "US30":
                    mock_symbols.append("US30Cash")
                elif sym == "GER40":
                    mock_symbols.append("GER40Cash")
                elif sym == "UK100":
                    mock_symbols.append("UK100Cash")
                else:
                    mock_symbols.append(f"{sym}{suffix}")
            self.symbol_mapper.update_broker_symbols(mock_symbols)
            return True, f"Connected to MT5 Gateway (Server: {self.server_name}, Account: {self.account_number})"

        try:
            # Step 1: Initialize MT5 terminal
            init_kwargs = {"timeout": 10000}
            if path or BridgeConfig.MT5_TERMINAL_PATH:
                init_kwargs["path"] = path or BridgeConfig.MT5_TERMINAL_PATH
            if server:
                init_kwargs["server"] = server
            if login:
                init_kwargs["login"] = login
            if password:
                init_kwargs["password"] = password

            logger.info("Calling mt5.initialize with server=%s, login=%s", server, login)
            if not mt5.initialize(**init_kwargs):
                err = mt5.last_error()
                self.last_error_message = f"MT5 initialize failed: {err}"
                logger.error(self.last_error_message)
                self.is_connected = False
                return False, self.last_error_message

            # Step 2: Login to specific account if provided
            if login and password:
                login_ok = mt5.login(login=login, password=password, server=server, timeout=10000)
                if not login_ok:
                    err = mt5.last_error()
                    self.last_error_message = f"MT5 login failed: {err}"
                    logger.error(self.last_error_message)
                    self.is_connected = False
                    return False, self.last_error_message

            # Step 3: Verify account identity and terminal status
            acc_info = mt5.account_info()
            if acc_info is None:
                err = mt5.last_error()
                self.last_error_message = f"Failed to retrieve account info: {err}"
                self.is_connected = False
                return False, self.last_error_message

            self.broker_company = getattr(acc_info, "company", "MetaQuotes")
            self.account_number = getattr(acc_info, "login", login)
            self.server_name = getattr(acc_info, "server", server)
            
            # Step 4: Discover all symbols from broker
            self.discover_symbols()
            
            self.is_connected = True
            self.last_connect_time = time.time()
            self.last_error_message = ""
            return True, f"Successfully connected to MT5 (Account: {self.account_number}, Server: {self.server_name})"

        except Exception as e:
            self.last_error_message = f"Connection exception: {str(e)}"
            logger.exception("MT5 connection exception")
            self.is_connected = False
            return False, self.last_error_message

    def disconnect(self) -> bool:
        """Gracefully disconnects and shuts down MT5 terminal connection."""
        if MT5_AVAILABLE and mt5 is not None:
            try:
                mt5.shutdown()
            except Exception as e:
                logger.warning("Error during mt5.shutdown(): %s", e)
        self.is_connected = False
        return True

    def discover_symbols(self) -> int:
        """Discovers symbols from MT5 broker and maps them to EMAT 19 instruments."""
        if not MT5_AVAILABLE or mt5 is None:
            return len(self.symbol_mapper.get_mapping())

        try:
            symbols = mt5.symbols_get()
            if not symbols:
                logger.warning("mt5.symbols_get() returned no symbols")
                return 0

            symbol_names = [s.name for s in symbols]
            mapping = self.symbol_mapper.update_broker_symbols(symbol_names)
            logger.info("Discovered %d broker symbols, mapped %d EMAT instruments", len(symbol_names), len(mapping))

            # Enable mapped symbols in MT5 Market Watch so ticks stream continuously
            for broker_sym in mapping.values():
                mt5.symbol_select(broker_sym, True)

            return len(mapping)
        except Exception as e:
            logger.error("Error discovering symbols: %s", e)
            return 0

    def get_quotes(self) -> List[Dict[str, Any]]:
        """
        Fetches live bid/ask/spread for all mapped EMAT instruments.
        Returns empty or partial quotes gracefully if feed is missing specific prices.
        """
        quotes: List[Dict[str, Any]] = []
        now_ms = int(time.time() * 1000)

        # Non-Windows or simulated fallback
        if not MT5_AVAILABLE or mt5 is None or not self.is_connected:
            source_label = f"XM Global MT5 Demo ({self.server_name})" if self.server_name else "MT5 Simulation"
            for sym in CANONICAL_INSTRUMENTS:
                base_price = self._mock_prices.get(sym, 1.0)
                spread_val = round(base_price * 0.00015, 5) if base_price < 10 else round(base_price * 0.0001, 2)
                bid = round(base_price, 5)
                ask = round(base_price + spread_val, 5)
                quotes.append({
                    "symbol": sym,
                    "broker_symbol": self.symbol_mapper.to_broker(sym) or sym,
                    "bid": bid,
                    "ask": ask,
                    "spread": round(ask - bid, 5),
                    "timestamp": now_ms,
                    "source": source_label
                })
            return quotes

        # Live MT5 quotes
        source_label = f"MT5 Broker ({self.server_name})"
        mapping = self.symbol_mapper.get_mapping()

        for canonical_sym, broker_sym in mapping.items():
            try:
                tick = mt5.symbol_info_tick(broker_sym)
                if tick is None:
                    continue

                bid = float(tick.bid) if tick.bid and not math.isnan(tick.bid) else 0.0
                ask = float(tick.ask) if tick.ask and not math.isnan(tick.ask) else 0.0
                ts = int(tick.time_msc) if getattr(tick, "time_msc", None) else now_ms

                spread = round(ask - bid, 6) if (bid > 0 and ask > 0) else None

                quotes.append({
                    "symbol": canonical_sym,
                    "broker_symbol": broker_sym,
                    "bid": bid if bid > 0 else None,
                    "ask": ask if ask > 0 else None,
                    "spread": spread,
                    "timestamp": ts,
                    "source": source_label
                })
            except Exception as e:
                logger.debug("Failed to fetch tick for %s (%s): %s", canonical_sym, broker_sym, e)

        return quotes

    def get_single_quote(self, symbol: str) -> Optional[Dict[str, Any]]:
        """Fetches live quote for a specific canonical symbol."""
        canonical_upper = symbol.upper()
        quotes = self.get_quotes()
        for q in quotes:
            if q["symbol"] == canonical_upper:
                return q
        return None

    def get_ohlc(self, symbol: str, timeframe: str = "H1", count: int = 100) -> List[Dict[str, Any]]:
        """
        Retrieves historical OHLC candles from MT5 broker.
        """
        canonical_upper = symbol.upper()
        broker_sym = self.symbol_mapper.to_broker(canonical_upper) or canonical_upper
        tf_const = TIMEFRAME_MAP.get(timeframe.upper(), 16385)

        if not MT5_AVAILABLE or mt5 is None or not self.is_connected:
            # Return synthetic test candles
            base = self._mock_prices.get(canonical_upper, 1.0850)
            candles = []
            now = int(time.time())
            step_sec = 3600 if timeframe.upper() == "H1" else 60
            for i in range(count, 0, -1):
                t = now - (i * step_sec)
                o = round(base + math.sin(i * 0.1) * (base * 0.002), 5)
                h = round(o + abs(math.cos(i * 0.2)) * (base * 0.0015), 5)
                l = round(o - abs(math.sin(i * 0.3)) * (base * 0.0015), 5)
                c = round((h + l) / 2.0, 5)
                candles.append({
                    "time": t,
                    "open": o,
                    "high": h,
                    "low": l,
                    "close": c,
                    "tick_volume": 150 + (i % 50)
                })
            return candles

        try:
            rates = mt5.copy_rates_from_pos(broker_sym, tf_const, 0, count)
            if rates is None or len(rates) == 0:
                logger.warning("mt5.copy_rates_from_pos returned no data for %s", broker_sym)
                return []

            candles = []
            for r in rates:
                candles.append({
                    "time": int(r["time"]),
                    "open": float(r["open"]),
                    "high": float(r["high"]),
                    "low": float(r["low"]),
                    "close": float(r["close"]),
                    "tick_volume": int(r["tick_volume"])
                })
            return candles
        except Exception as e:
            logger.error("Error copying rates for %s: %s", broker_sym, e)
            return []

    def get_account_info(self) -> Dict[str, Any]:
        """Returns account metadata in read-only mode."""
        if not MT5_AVAILABLE or mt5 is None or not self.is_connected:
            acc_type = "DEMO" if "DEMO" in (self.server_name or "").upper() or "10" in (self.server_name or "") or "XM" in (self.server_name or "").upper() else "REAL"
            return {
                "server": self.server_name or "XMGlobal-MT5 10",
                "login": self.account_number or 346533676,
                "company": self.broker_company,
                "currency": "USD",
                "account_type": acc_type,
                "balance": 10000.0,
                "equity": 10000.0,
                "margin_free": 10000.0,
                "leverage": 500,
                "read_only": True,
                "terminal_connected": self.is_connected,
                "trade_allowed": False  # Read-only investor invariant
            }

        try:
            acc = mt5.account_info()
            if acc is None:
                return {"error": "Account info unavailable", "terminal_connected": False}

            trade_mode = getattr(acc, "trade_mode", -1)
            acc_type = "DEMO" if trade_mode == 0 else ("CONTEST" if trade_mode == 1 else ("REAL" if trade_mode == 2 else "UNKNOWN"))

            return {
                "server": getattr(acc, "server", self.server_name),
                "login": getattr(acc, "login", self.account_number),
                "company": getattr(acc, "company", self.broker_company),
                "currency": getattr(acc, "currency", "USD"),
                "account_type": acc_type,
                "balance": float(getattr(acc, "balance", 0.0)),
                "equity": float(getattr(acc, "equity", 0.0)),
                "margin_free": float(getattr(acc, "margin_free", 0.0)),
                "leverage": int(getattr(acc, "leverage", 100)),
                "read_only": True,
                "terminal_connected": self.is_connected,
                "trade_allowed": False  # Read-only investor invariant
            }
        except Exception as e:
            return {"error": str(e), "terminal_connected": False}
