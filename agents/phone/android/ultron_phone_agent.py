"""Ultron Android phone agent (Termux) — Section 11.7 / device control.

Runs on the phone in Termux, holds a persistent WebSocket to the Ultron backend at
ws://<host>:8080/ws/device/android, and executes real device actions via `am`/`input`/`cmd` and the
Termux:API. READ actions run instantly; anything above READ requires an approvalToken issued by the
backend ApprovalGate (calls/messages are CRITICAL → backend also voice-verifies before sending one).

Setup:  bash setup.sh   (installs termux-api, deps, starts the agent)
Run:    export ULTRON_WS=ws://<laptop-ip>:8080/ws/device/android ; python ultron_phone_agent.py
"""
from __future__ import annotations

import json
import os
import subprocess
import time

try:
    import websocket  # websocket-client
except Exception:  # noqa: BLE001
    websocket = None

WS_URL = os.getenv("ULTRON_WS", "ws://localhost:8080/ws/device/android")

CAPABILITIES = [
    "open_app", "call", "message", "lock", "screenshot", "alarm", "media_play",
    "flashlight", "notifications", "battery", "dnd", "navigate",
]
# Actions that change state / touch contacts — require an approval token from the backend.
GATED = {"call", "message", "navigate", "alarm", "dnd", "flashlight", "lock", "media_play", "type"}

APP_PACKAGES = {
    "youtube": "com.google.android.youtube",
    "whatsapp": "com.whatsapp",
    "camera": "com.android.camera",
    "maps": "com.google.android.apps.maps",
    "spotify": "com.spotify.music",
    "chrome": "com.android.chrome",
    "gmail": "com.google.android.gm",
}


def sh(args, timeout=20):
    try:
        out = subprocess.run(args, capture_output=True, text=True, timeout=timeout)
        return {"ok": out.returncode == 0, "out": out.stdout.strip(), "err": out.stderr.strip()}
    except Exception as exc:  # noqa: BLE001
        return {"ok": False, "out": "", "err": str(exc)}


def execute(action: str, args: dict) -> dict:
    a = args or {}
    if action == "open_app":
        pkg = APP_PACKAGES.get(str(a.get("app", "")).lower().strip(), a.get("app"))
        return sh(["am", "start", "-n", f"{pkg}/.MainActivity"]) if pkg and "/" not in str(pkg) \
            else sh(["monkey", "-p", str(pkg), "-c", "android.intent.category.LAUNCHER", "1"])
    if action == "battery":
        return sh(["termux-battery-status"])
    if action == "notifications":
        return sh(["termux-notification-list"])
    if action == "screenshot":
        path = "/sdcard/ultron_shot.png"
        sh(["screencap", "-p", path])
        return {"ok": True, "out": f"screenshot saved {path}"}
    if action == "lock":
        return sh(["input", "keyevent", "26"])  # KEYCODE_POWER
    if action == "flashlight":
        return sh(["termux-torch", "on" if a.get("on") else "off"])
    if action == "dnd":
        return sh(["cmd", "notification", "set_dnd", "priority" if a.get("on") else "off"])
    if action == "media_play":
        return sh(["input", "keyevent", "126"])  # MEDIA_PLAY
    if action == "alarm":
        # parse a simple time like "6am" from raw
        raw = str(a.get("raw", ""))
        hour = 6
        for tok in raw.replace("am", " am").replace("pm", " pm").split():
            if tok.isdigit():
                hour = int(tok)
        return sh(["am", "start", "-a", "android.intent.action.SET_ALARM",
                   "--ei", "android.intent.extra.alarm.HOUR", str(hour),
                   "--ei", "android.intent.extra.alarm.MINUTES", "0"])
    if action == "call":
        contact = str(a.get("contact", "")).strip()
        # Resolve via termux-contact-list; here we dial the raw if it's a number.
        return sh(["am", "start", "-a", "android.intent.action.CALL", "-d", f"tel:{contact}"])
    if action == "message":
        return sh(["am", "start", "-a", "android.intent.action.SENDTO",
                   "-d", "smsto:", "--es", "sms_body", str(a.get("raw", ""))])
    if action == "navigate":
        dest = str(a.get("destination", "")).strip().replace(" ", "+")
        return sh(["am", "start", "-a", "android.intent.action.VIEW",
                   "-d", f"google.navigation:q={dest}"])
    return {"ok": False, "err": f"unknown action {action}"}


def on_message(ws, message):
    try:
        cmd = json.loads(message)
    except Exception:  # noqa: BLE001
        return
    action = cmd.get("action", "")
    args = cmd.get("args", {})
    token = cmd.get("approvalToken", "")
    if action in GATED and not token:
        result = {"ok": False, "err": f"{action} requires backend approval"}
    else:
        result = execute(action, args)
    ws.send(json.dumps({"type": "result", "id": cmd.get("id"), "result": result}))


def on_open(ws):
    ws.send(json.dumps({"type": "register", "capabilities": CAPABILITIES}))
    print(f"[ultron-phone] connected → {WS_URL}")


def main():
    if websocket is None:
        print("Install the client: pip install websocket-client")
        return
    while True:
        try:
            ws = websocket.WebSocketApp(WS_URL, on_open=on_open, on_message=on_message)
            ws.run_forever(ping_interval=20)
        except Exception as exc:  # noqa: BLE001
            print(f"[ultron-phone] reconnecting after error: {exc}")
        time.sleep(5)


if __name__ == "__main__":
    main()
