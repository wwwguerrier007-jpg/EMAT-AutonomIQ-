#!/usr/bin/env python3
"""
EMAT AutonomIQ — Token Generator & Bridge Registration Utility
Generates cryptographically strong 256-bit URL-safe authentication tokens and
automatically registers the bridge configuration for EMAT AutonomIQ.
"""
import json
import os
import secrets
import sys

def main():
    token = secrets.token_urlsafe(32)
    base_dir = os.path.dirname(os.path.abspath(__file__))
    token_file = os.path.join(base_dir, ".bridge_token")
    config_file = os.path.join(base_dir, "bridge_config.json")
    
    # Save raw token
    with open(token_file, "w") as f:
        f.write(token)
    
    # Generate registration descriptor
    reg_data = {
        "bridge_url": os.getenv("BRIDGE_PUBLIC_URL", "http://10.0.2.2:8080"),
        "bridge_token": token,
        "broker_name": "XM Global",
        "broker_server": "XMGlobal-MT5 10",
        "account_number": "346533676",
        "account_type": "DEMO",
        "investor_password": "@Guerrier#19940",
        "version": "1.0.4",
        "deployed": True,
        "port": int(os.getenv("BRIDGE_PORT", "8080"))
    }
    
    with open(config_file, "w") as f:
        json.dump(reg_data, f, indent=2)
        
    # Also sync to Android app assets if present
    assets_config = os.path.join(base_dir, "..", "app", "src", "main", "assets", "bridge_config.json")
    if os.path.exists(os.path.dirname(assets_config)):
        try:
            with open(assets_config, "w") as f:
                json.dump(reg_data, f, indent=2)
            print("[INFO] Synced bridge registration to app assets.")
        except Exception as e:
            print(f"[WARN] Could not sync to assets: {e}")

    print("\n" + "=" * 64)
    print(" EMAT AutonomIQ — MT5 Bridge Automatic Registration")
    print("=" * 64)
    print("\nBridge Authentication Token Generated & Registered Successfully:\n")
    print(f"  Token:      {token}")
    print(f"  Bridge URL: {reg_data['bridge_url']}")
    print(f"  Broker:     {reg_data['broker_name']} ({reg_data['broker_server']})")
    print(f"  Account:    {reg_data['account_number']}\n")
    print("EMAT AutonomIQ will automatically detect and populate these values.")
    print("No manual copying of technical connection details is required.")
    print("=" * 64 + "\n")

if __name__ == "__main__":
    main()
