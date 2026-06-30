"""Ultron Android agent (Termux ADB bridge) — Section 11.7.

Runs inside Termux on Android. Connects to the Ultron backend, relays device context (notifications,
location, battery) up, and executes a small allowlist of actions down (launch app, toggle settings)
via `adb shell` / Termux APIs. Mutating actions require an approval token from the backend, mirroring
the laptop agent's manifest. Contact-touching actions (SMS/call) stay disabled — Twilio extension
point only.

Setup (on the phone):
  pkg install python termux-api android-tools
  export ULTRON_BACKEND=http://<laptop-ip>:8080
  python termux_agent.py
"""
from __future__ import annotations

import json
import os
import subprocess
import time
from typing import Optional

try:
    import requests
except Exception:  # noqa: BLE001
    requests = None

BACKEND = os.getenv("ULTRON_BACKEND", "http://localhost:8080")
DEVICE_ID = os.getenv("ULTRON_DEVICE_ID", "android-termux")
POLL_SECONDS = int(os.getenv("ULTRON_POLL_SECONDS", "5"))

# Allowlisted actions and their risk (the backend gate is the source of truth; this is defence in depth).
ALLOWED = {
    "app.launch": "LOW",
    "notify": "LOW",
    "settings.toggle": "HIGH",
    "location.read": "READ",
    "battery.read": "READ",
}
FORBIDDEN = {"sms.send", "call.place", "factory.reset"}


def sh(args: list[str], timeout: int = 20) -> dict:
    try:
        out = subprocess.run(args, capture_output=True, text=True, timeout=timeout)
        return {"ok": out.returncode == 0, "stdout": out.stdout.strip(), "stderr": out.stderr.strip()}
    except Exception as exc:  # noqa: BLE001
        return {"ok": False, "stdout": "", "stderr": str(exc)}


# --- Termux API context (relayed up to the backend) ---
def battery() -> dict:
    return sh(["termux-battery-status"])


def location() -> dict:
    return sh(["termux-location", "-p", "network"])


def notify(title: str, content: str) -> dict:
    return sh(["termux-notification", "--title", title, "--content", content])


# --- Action execution (down from the backend) ---
def execute(action: str, args: dict, approval_token: Optional[str]) -> dict:
    if action in FORBIDDEN:
        return {"ok": False, "error": f"{action} is forbidden on this device"}
    risk = ALLOWED.get(action)
    if risk is None:
        return {"ok": False, "error": f"{action} not in allowlist"}
    if risk != "READ" and not approval_token:
        return {"ok": False, "error": f"{action} is {risk}; requires backend approval token"}

    if action == "app.launch":
        pkg = args.get("package", "")
        return sh(["am", "start", "-n", pkg]) if pkg else {"ok": False, "error": "package required"}
    if action == "notify":
        return notify(args.get("title", "Ultron"), args.get("content", ""))
    if action == "settings.toggle":
        # e.g. enable Do Not Disturb via cmd; requires permissions.
        return sh(["cmd", "notification", "set_dnd", args.get("mode", "priority")])
    if action == "location.read":
        return location()
    if action == "battery.read":
        return battery()
    return {"ok": False, "error": "unhandled"}


def poll_once() -> None:
    if requests is None:
        print("[android-agent] 'requests' not installed (pip install requests)")
        return
    try:
        # Relay context up.
        requests.post(f"{BACKEND}/api/webhook", json={
            "source": DEVICE_ID, "event": "context",
            "payload": json.dumps({"battery": battery().get("stdout", "")})[:1000],
        }, timeout=5)
        # Pull pending actions (backend returns [] if none / endpoint absent).
        r = requests.get(f"{BACKEND}/api/agent/android/pending", params={"deviceId": DEVICE_ID}, timeout=5)
        if r.status_code == 200:
            for cmd in r.json().get("actions", []):
                res = execute(cmd.get("action", ""), cmd.get("args", {}), cmd.get("approvalToken"))
                requests.post(f"{BACKEND}/api/agent/android/result",
                              json={"id": cmd.get("id"), "result": res}, timeout=5)
    except Exception as exc:  # noqa: BLE001
        print(f"[android-agent] poll error: {exc}")


def main() -> None:
    print(f"[android-agent] {DEVICE_ID} → {BACKEND}, polling every {POLL_SECONDS}s")
    while True:
        poll_once()
        time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    main()
