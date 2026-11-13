"""
EMAT AutonomIQ — Security & Authentication Middleware
Protects bridge endpoints using Bearer token verification with constant-time comparison.
"""
import hmac
from typing import Optional
from config import BridgeConfig

def verify_token(provided_token: Optional[str]) -> bool:
    """
    Verifies Bearer token using constant-time comparison to prevent timing attacks.
    If provided_token starts with 'Bearer ', it strips the prefix.
    """
    if not provided_token:
        return False
    
    clean_token = provided_token.strip()
    if clean_token.lower().startswith("bearer "):
        clean_token = clean_token[7:].strip()
        
    expected_token = BridgeConfig.get_or_create_token().strip()
    if not expected_token:
        return False
        
    return hmac.compare_digest(clean_token, expected_token)
