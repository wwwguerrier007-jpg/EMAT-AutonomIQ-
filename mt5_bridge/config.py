"""
EMAT AutonomIQ — MT5 Python Bridge Configuration
Handles environment settings, credentials, server parameters, and paths.
"""
import os
import secrets
from typing import Optional

class BridgeConfig:
    HOST: str = os.getenv("BRIDGE_HOST", "0.0.0.0")
    PORT: int = int(os.getenv("BRIDGE_PORT", "8080"))
    
    # Secret Bearer Token for securing API access.
    # If not set in environment, a default token is generated or read from token file.
    BRIDGE_API_TOKEN: str = os.getenv("BRIDGE_API_TOKEN", "")
    
    # MT5 Broker connection credentials (optional defaults, can also be supplied via /connection/connect)
    MT5_SERVER: str = os.getenv("MT5_SERVER", "")
    MT5_LOGIN: int = int(os.getenv("MT5_LOGIN", "0")) if os.getenv("MT5_LOGIN", "0").isdigit() else 0
    MT5_INVESTOR_PASSWORD: str = os.getenv("MT5_INVESTOR_PASSWORD", "")
    
    # Custom terminal path (if not standard default installed location)
    MT5_TERMINAL_PATH: Optional[str] = os.getenv("MT5_TERMINAL_PATH", None)
    
    # SSL/TLS Configuration for HTTPS
    SSL_CERT_FILE: Optional[str] = os.getenv("SSL_CERT_FILE", None)
    SSL_KEY_FILE: Optional[str] = os.getenv("SSL_KEY_FILE", None)
    
    # Tick polling interval in seconds
    POLL_INTERVAL_SEC: float = float(os.getenv("POLL_INTERVAL_SEC", "1.0"))
    
    # Simulation mode override (force mock ticks for local non-Windows test runners)
    FORCE_MOCK: bool = os.getenv("FORCE_MOCK", "false").lower() in ("true", "1", "yes")

    @classmethod
    def get_or_create_token(cls) -> str:
        if cls.BRIDGE_API_TOKEN:
            return cls.BRIDGE_API_TOKEN
        
        token_file = os.path.join(os.path.dirname(__file__), ".bridge_token")
        if os.path.exists(token_file):
            try:
                with open(token_file, "r") as f:
                    cls.BRIDGE_API_TOKEN = f.read().strip()
                    if cls.BRIDGE_API_TOKEN:
                        return cls.BRIDGE_API_TOKEN
            except Exception:
                pass
        
        # Generate new 256-bit secure URL-safe token
        new_token = secrets.token_urlsafe(32)
        cls.BRIDGE_API_TOKEN = new_token
        try:
            with open(token_file, "w") as f:
                f.write(new_token)
        except Exception:
            pass
        return new_token
