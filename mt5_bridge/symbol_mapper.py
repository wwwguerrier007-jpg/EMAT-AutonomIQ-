"""
EMAT AutonomIQ — Dynamic MT5 Symbol Mapper
Maps broker-specific symbols (e.g. HFM suffixes like EURUSDm, EURUSD.pro, indices aliases like USTEC, USA500)
to the canonical 19 EMAT Instrument Registry.
"""
from typing import Dict, List, Optional, Set

# Canonical 19 instruments defined by EMAT AutonomIQ
CANONICAL_INSTRUMENTS: List[str] = [
    # Forex (10)
    "EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD",
    "AUDUSD", "NZDUSD", "EURGBP", "EURJPY", "GBPJPY",
    # Metals (2)
    "XAUUSD", "XAGUSD",
    # Indices (5)
    "NAS100", "US500", "US30", "GER40", "UK100",
    # Crypto (2)
    "BTCUSD", "ETHUSD"
]

# Alias patterns per canonical instrument
SYMBOL_ALIASES: Dict[str, List[str]] = {
    # Forex
    "EURUSD": ["EURUSD"],
    "GBPUSD": ["GBPUSD"],
    "USDJPY": ["USDJPY"],
    "USDCHF": ["USDCHF"],
    "USDCAD": ["USDCAD"],
    "AUDUSD": ["AUDUSD"],
    "NZDUSD": ["NZDUSD"],
    "EURGBP": ["EURGBP"],
    "EURJPY": ["EURJPY"],
    "GBPJPY": ["GBPJPY"],
    # Metals
    "XAUUSD": ["XAUUSD", "GOLD"],
    "XAGUSD": ["XAGUSD", "SILVER"],
    # Indices
    "NAS100": ["NAS100", "USTEC", "USA100", "US100", "NDX100", "TECH100"],
    "US500": ["US500", "SPX500", "USA500", "US500.CASH", "SP500"],
    "US30": ["US30", "DJ30", "USA30", "WALLSTREET", "US30.CASH"],
    "GER40": ["GER40", "GER30", "DE40", "DE30", "DAX40", "DAX30", "GER40.CASH"],
    "UK100": ["UK100", "FTSE100", "GB100", "UK100.CASH"],
    # Crypto
    "BTCUSD": ["BTCUSD", "BITCOIN", "BTCUSDT"],
    "ETHUSD": ["ETHUSD", "ETHEREUM", "ETHUSDT"],
}

# Standard broker suffixes to strip when normalizing (HFM, XM Global, IC Markets, Exness)
KNOWN_SUFFIXES = [
    "M", ".PRO", ".M", ".RAW", ".ECN", "#", ".B", "B", "_I", ".CASH", ".A", ".C", "_M",
    "MICRO", ".MICRO", "CASH", ".CASH"
]

class SymbolMapper:
    def __init__(self):
        # Maps canonical -> broker symbol (e.g. "EURUSD" -> "EURUSDm")
        self._canonical_to_broker: Dict[str, str] = {}
        # Maps broker symbol -> canonical (e.g. "EURUSDm" -> "EURUSD")
        self._broker_to_canonical: Dict[str, str] = {}
        # All discovered broker symbols
        self._all_broker_symbols: Set[str] = set()

    def update_broker_symbols(self, broker_symbols: List[str]) -> Dict[str, str]:
        """
        Discovers and maps the 19 canonical symbols against the broker's active symbol list.
        Prioritizes exact matches, then base name + known suffix/prefix matches.
        """
        self._all_broker_symbols = set(broker_symbols)
        self._canonical_to_broker.clear()
        self._broker_to_canonical.clear()

        # Build lookup table for quick resolution: uppercase cleaned -> original broker symbol
        upper_broker_symbols = {s.upper(): s for s in broker_symbols}

        for canonical in CANONICAL_INSTRUMENTS:
            aliases = SYMBOL_ALIASES.get(canonical, [canonical])
            matched_broker_sym = None

            # 1. First priority: Exact match of any alias
            for alias in aliases:
                if alias in upper_broker_symbols:
                    matched_broker_sym = upper_broker_symbols[alias]
                    break

            # 2. Second priority: Alias with common suffix (e.g. EURUSDm, EURUSD.pro, USTEC.cash)
            if not matched_broker_sym:
                for alias in aliases:
                    for s_upper, orig_sym in upper_broker_symbols.items():
                        # Check prefix match where remaining part is known suffix or punctuation
                        if s_upper.startswith(alias):
                            remainder = s_upper[len(alias):]
                            if remainder in KNOWN_SUFFIXES or len(remainder) <= 4:
                                matched_broker_sym = orig_sym
                                break
                    if matched_broker_sym:
                        break

            # 3. Third priority: Normalized name match (strip '.', '_', '#', 'm', 'pro')
            if not matched_broker_sym:
                for alias in aliases:
                    for s_upper, orig_sym in upper_broker_symbols.items():
                        cleaned = s_upper.replace(".", "").replace("_", "").replace("#", "")
                        for suffix in ["M", "PRO", "RAW", "ECN", "CASH", "B"]:
                            if cleaned.endswith(suffix):
                                cleaned = cleaned[:-len(suffix)]
                        if cleaned == alias:
                            matched_broker_sym = orig_sym
                            break
                    if matched_broker_sym:
                        break

            if matched_broker_sym:
                self._canonical_to_broker[canonical] = matched_broker_sym
                self._broker_to_canonical[matched_broker_sym] = canonical

        return self.get_mapping()

    def to_broker(self, canonical_sym: str) -> Optional[str]:
        """Returns the broker's symbol name for the given canonical instrument."""
        canonical_upper = canonical_sym.upper()
        if canonical_upper in self._canonical_to_broker:
            return self._canonical_to_broker[canonical_upper]
        # Fallback to direct symbol if broker has it directly
        if canonical_upper in self._all_broker_symbols:
            return canonical_upper
        return None

    def to_canonical(self, broker_sym: str) -> Optional[str]:
        """Returns the canonical EMAT instrument for a broker symbol."""
        if broker_sym in self._broker_to_canonical:
            return self._broker_to_canonical[broker_sym]
        upper = broker_sym.upper()
        if upper in self._broker_to_canonical:
            return self._broker_to_canonical[upper]
        if upper in CANONICAL_INSTRUMENTS:
            return upper
        return None

    def get_mapping(self) -> Dict[str, str]:
        """Returns the current canonical -> broker mapping dictionary."""
        return dict(self._canonical_to_broker)

    def get_coverage_summary(self) -> Dict[str, any]:
        """Returns mapping statistics and coverage percentage against EMAT 19."""
        mapped_count = len(self._canonical_to_broker)
        total_canonical = len(CANONICAL_INSTRUMENTS)
        coverage_pct = round((mapped_count / total_canonical) * 100.0, 1) if total_canonical > 0 else 0.0
        missing = [c for c in CANONICAL_INSTRUMENTS if c not in self._canonical_to_broker]
        return {
            "total_canonical": total_canonical,
            "mapped_count": mapped_count,
            "coverage_percent": coverage_pct,
            "missing_instruments": missing,
            "mappings": self._canonical_to_broker
        }
