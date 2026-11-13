"""
EMAT AutonomIQ — MT5 Python Bridge Automated Test Suite
Tests authentication, dynamic symbol discovery/mapping, read-only invariants,
quotes calculation, OHLC history, and HTTP REST endpoints.
"""
import json
import os
import sys
import threading
import time
import unittest
import urllib.request
import urllib.error

# Ensure parent directory is in Python path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from auth import verify_token
from config import BridgeConfig
from mt5_manager import MT5Manager
from symbol_mapper import CANONICAL_INSTRUMENTS, SymbolMapper
from server import run_standalone_server


class TestSymbolMapper(unittest.TestCase):
    def setUp(self):
        self.mapper = SymbolMapper()

    def test_exact_symbol_mapping(self):
        """Tests that exact symbol matches map directly to canonical names."""
        broker_symbols = list(CANONICAL_INSTRUMENTS)
        mapping = self.mapper.update_broker_symbols(broker_symbols)
        self.assertEqual(len(mapping), 19)
        for sym in CANONICAL_INSTRUMENTS:
            self.assertEqual(self.mapper.to_broker(sym), sym)
            self.assertEqual(self.mapper.to_canonical(sym), sym)

    def test_hfm_suffixes(self):
        """Tests HF Markets (HFM) suffix patterns: EURUSDm, EURUSD.pro, EURUSD#, etc."""
        hfm_symbols = [
            "EURUSDm", "GBPUSDm", "USDJPYm", "USDCHFm", "USDCADm",
            "AUDUSDm", "NZDUSDm", "EURGBPm", "EURJPYm", "GBPJPYm",
            "XAUUSDm", "XAGUSDm",
            "USTECm", "USA500m", "USA30m", "GER40m", "UK100m",
            "BTCUSDm", "ETHUSDm"
        ]
        mapping = self.mapper.update_broker_symbols(hfm_symbols)
        self.assertEqual(len(mapping), 19)
        self.assertEqual(self.mapper.to_broker("EURUSD"), "EURUSDm")
        self.assertEqual(self.mapper.to_broker("NAS100"), "USTECm")
        self.assertEqual(self.mapper.to_broker("US500"), "USA500m")
        self.assertEqual(self.mapper.to_broker("US30"), "USA30m")
        self.assertEqual(self.mapper.to_canonical("EURUSDm"), "EURUSD")
        self.assertEqual(self.mapper.to_canonical("USTECm"), "NAS100")

    def test_index_and_metals_aliases(self):
        """Tests alternative broker naming (e.g. GOLD, SILVER, SPX500, DJ30, DE40)."""
        broker_symbols = [
            "EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD",
            "AUDUSD", "NZDUSD", "EURGBP", "EURJPY", "GBPJPY",
            "GOLD", "SILVER",
            "TECH100", "SPX500", "DJ30", "DE40", "FTSE100",
            "BITCOIN", "ETHEREUM"
        ]
        mapping = self.mapper.update_broker_symbols(broker_symbols)
        self.assertEqual(len(mapping), 19)
        self.assertEqual(self.mapper.to_broker("XAUUSD"), "GOLD")
        self.assertEqual(self.mapper.to_broker("XAGUSD"), "SILVER")
        self.assertEqual(self.mapper.to_broker("NAS100"), "TECH100")
        self.assertEqual(self.mapper.to_broker("US500"), "SPX500")
        self.assertEqual(self.mapper.to_broker("US30"), "DJ30")
        self.assertEqual(self.mapper.to_broker("GER40"), "DE40")
        self.assertEqual(self.mapper.to_broker("UK100"), "FTSE100")
        self.assertEqual(self.mapper.to_broker("BTCUSD"), "BITCOIN")
        self.assertEqual(self.mapper.to_broker("ETHUSD"), "ETHEREUM")

    def test_coverage_calculation(self):
        """Tests that coverage summary accurately reports percentage and missing list."""
        partial_symbols = ["EURUSD", "GBPUSD", "USDJPY"]
        self.mapper.update_broker_symbols(partial_symbols)
        coverage = self.mapper.get_coverage_summary()
        self.assertEqual(coverage["total_canonical"], 19)
        self.assertEqual(coverage["mapped_count"], 3)
        self.assertAlmostEqual(coverage["coverage_percent"], 15.8, places=1)
        self.assertIn("NAS100", coverage["missing_instruments"])


class TestAuth(unittest.TestCase):
    def setUp(self):
        BridgeConfig.BRIDGE_API_TOKEN = "test_secret_token_1234567890_abcdef"

    def test_valid_token(self):
        self.assertTrue(verify_token("test_secret_token_1234567890_abcdef"))
        self.assertTrue(verify_token("Bearer test_secret_token_1234567890_abcdef"))
        self.assertTrue(verify_token("bearer test_secret_token_1234567890_abcdef"))

    def test_invalid_token(self):
        self.assertFalse(verify_token("wrong_token"))
        self.assertFalse(verify_token(""))
        self.assertFalse(verify_token(None))
        self.assertFalse(verify_token("Bearer wrong_token"))


class TestMT5Manager(unittest.TestCase):
    def setUp(self):
        self.manager = MT5Manager()
        self.manager.connect(server="HFMarketsSV-Live01", login=1002003, password="ReadOnlyInvestor123")

    def test_connection_state(self):
        self.assertTrue(self.manager.is_connected)
        self.assertEqual(self.manager.server_name, "HFMarketsSV-Live01")
        self.assertEqual(self.manager.account_number, 1002003)

    def test_read_only_invariants(self):
        """Verifies strict read-only compliance: trade_allowed must be False and read_only True."""
        acc = self.manager.get_account_info()
        self.assertTrue(acc.get("read_only"))
        self.assertFalse(acc.get("trade_allowed"))
        # Check that no trading execution methods exist on MT5Manager
        self.assertFalse(hasattr(self.manager, "order_send"))
        self.assertFalse(hasattr(self.manager, "place_order"))
        self.assertFalse(hasattr(self.manager, "close_position"))

    def test_quotes_structure(self):
        """Verifies quotes contain required fields for all 19 canonical instruments."""
        quotes = self.manager.get_quotes()
        self.assertGreaterEqual(len(quotes), 19)
        symbols = {q["symbol"] for q in quotes}
        for sym in CANONICAL_INSTRUMENTS:
            self.assertIn(sym, symbols)
        
        eurusd = next(q for q in quotes if q["symbol"] == "EURUSD")
        self.assertIn("bid", eurusd)
        self.assertIn("ask", eurusd)
        self.assertIn("spread", eurusd)
        self.assertIn("timestamp", eurusd)
        self.assertIn("source", eurusd)
        self.assertGreater(eurusd["bid"], 0.0)
        self.assertGreater(eurusd["ask"], eurusd["bid"])
        self.assertAlmostEqual(eurusd["spread"], eurusd["ask"] - eurusd["bid"], places=5)

    def test_ohlc_candles(self):
        """Verifies OHLC history retrieval."""
        candles = self.manager.get_ohlc("EURUSD", "H1", 50)
        self.assertEqual(len(candles), 50)
        first = candles[0]
        self.assertIn("open", first)
        self.assertIn("high", first)
        self.assertIn("low", first)
        self.assertIn("close", first)
        self.assertIn("tick_volume", first)
        self.assertGreaterEqual(first["high"], first["low"])


class TestHttpServerEndpoints(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        BridgeConfig.BRIDGE_API_TOKEN = "suite_test_token_987654321"
        cls.port = 8899
        cls.base_url = f"http://127.0.0.1:{cls.port}"
        cls.server_thread = threading.Thread(target=run_standalone_server, kwargs={"port": cls.port}, daemon=True)
        cls.server_thread.start()
        time.sleep(0.5)  # Allow server to bind

    def _make_request(self, path: str, token: str = "suite_test_token_987654321", method: str = "GET", data: dict = None):
        url = f"{self.base_url}{path}"
        headers = {}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        req_data = None
        if data:
            req_data = json.dumps(data).encode("utf-8")
            headers["Content-Type"] = "application/json"

        req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=5) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, json.loads(body)

    def test_health_authenticated(self):
        # Test /health
        status, data = self._make_request("/health")
        self.assertEqual(status, 200)
        self.assertEqual(data["status"], "OK")
        self.assertTrue(data["readOnly"])
        self.assertEqual(data["version"], "1.0.4")

        # Test /api/v1/health (Android Mt5BridgeClient path)
        status, data = self._make_request("/api/v1/health")
        self.assertEqual(status, 200)
        self.assertEqual(data["status"], "OK")

    def test_unauthorized_rejection(self):
        url = f"{self.base_url}/health"
        req = urllib.request.Request(url, headers={"Authorization": "Bearer bad_token"})
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            urllib.request.urlopen(req, timeout=5)
        self.assertEqual(ctx.exception.code, 401)

    def test_quotes_endpoint(self):
        # Test /market/quotes
        status, data = self._make_request("/market/quotes")
        self.assertEqual(status, 200)
        self.assertIn("quotes", data)
        self.assertGreaterEqual(data["count"], 19)

        # Test /api/v1/quotes (Android Mt5BridgeClient path)
        status, data = self._make_request("/api/v1/quotes")
        self.assertEqual(status, 200)
        self.assertIn("quotes", data)

    def test_single_quote_endpoint(self):
        status, data = self._make_request("/market/quote/EURUSD")
        self.assertEqual(status, 200)
        self.assertEqual(data["symbol"], "EURUSD")
        self.assertGreater(data["bid"], 0.0)

    def test_ohlc_endpoint(self):
        status, data = self._make_request("/market/ohlc/NAS100?timeframe=H1&count=20")
        self.assertEqual(status, 200)
        self.assertEqual(data["symbol"], "NAS100")
        self.assertEqual(data["count"], 20)
        self.assertEqual(len(data["candles"]), 20)

    def test_connect_and_status(self):
        connect_data = {
            "server": "HFMarketsSV-Live02",
            "account": 88776655,
            "investor_password": "InvestorPassDemo"
        }
        status, data = self._make_request("/connection/connect", method="POST", data=connect_data)
        self.assertEqual(status, 200)
        self.assertTrue(data["success"])
        self.assertEqual(data["server"], "HFMarketsSV-Live02")

        status, status_data = self._make_request("/connection/status")
        self.assertEqual(status, 200)
        self.assertEqual(status_data["server"], "HFMarketsSV-Live02")
        self.assertEqual(status_data["accountNumber"], 88776655)
        self.assertTrue(status_data["readOnly"])

    def test_xm_global_demo_connect_and_status(self):
        connect_data = {
            "server": "XMGlobal-MT5 10",
            "account": 346533676,
            "investor_password": "@Guerrier#19940"
        }
        status, data = self._make_request("/connection/connect", method="POST", data=connect_data)
        self.assertEqual(status, 200)
        self.assertTrue(data["success"])
        self.assertEqual(data["server"], "XMGlobal-MT5 10")
        self.assertEqual(data["accountNumber"], 346533676)

        status, status_data = self._make_request("/connection/status")
        self.assertEqual(status, 200)
        self.assertTrue(status_data["connected"])
        self.assertEqual(status_data["server"], "XMGlobal-MT5 10")
        self.assertEqual(status_data["accountNumber"], 346533676)
        self.assertEqual(status_data["accountType"], "DEMO")
        self.assertTrue(status_data["readOnly"])
        self.assertFalse(status_data["tradeAllowed"])

        status, qdata = self._make_request("/market/quotes")
        self.assertEqual(status, 200)
        self.assertGreaterEqual(qdata["count"], 19)
        self.assertIn("XM Global", qdata["quotes"][0]["source"])


if __name__ == "__main__":
    unittest.main()
