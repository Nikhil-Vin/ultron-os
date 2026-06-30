"""Voice tools (Section 9.9.F / Phase 2). Each returns BOTH a spoken summary and a typed HUD
render event matching voice/contract.md. In DATA_MODE=demo they read voice/seed/*.json; otherwise
they call the Java backend.
"""
from __future__ import annotations

import json
import os
import time
from pathlib import Path
from typing import Tuple

import httpx

BACKEND = os.getenv("ULTRON_BACKEND_URL", "http://localhost:8080")
DATA_MODE = os.getenv("ULTRON_DATA_MODE", "live")  # live | demo
SEED = Path(__file__).parent / "seed"


def _now() -> int:
    return int(time.time() * 1000)


def _event(etype: str, payload: dict) -> dict:
    return {"type": etype, "ts": _now(), "payload": payload}


def _seed(name: str) -> dict:
    p = SEED / name
    return json.loads(p.read_text()) if p.exists() else {}


def get_daily_brief() -> Tuple[str, dict]:
    if DATA_MODE == "demo":
        data = _seed("daily_brief.json")
        lines = data.get("lines", [])
    else:
        try:
            r = httpx.post(f"{BACKEND}/api/brief", timeout=30)
            brief = r.json().get("brief", "")
            lines = [ln for ln in brief.splitlines() if ln.strip()][:8]
        except Exception:  # noqa: BLE001
            lines = ["Couldn't reach the backend for the brief."]
    spoken = "Here's your brief. " + " ".join(lines[:3])
    return spoken, _event("brief", {"title": "Brief", "lines": lines})


def show_metrics() -> Tuple[str, dict]:
    data = _seed("metrics.json") if DATA_MODE == "demo" else {
        "title": "P&L (7d)", "unit": "₹",
        "series": [{"label": d, "value": v} for d, v in
                   zip(["Mon", "Tue", "Wed", "Thu", "Fri"], [1200, -300, 800, 1500, -200])],
    }
    total = sum(p["value"] for p in data.get("series", []))
    spoken = f"Your {data.get('title', 'metrics')} nets {data.get('unit', '')}{total}."
    return spoken, _event("metrics", data)


def get_pipeline() -> Tuple[str, dict]:
    data = _seed("pipeline.json") if DATA_MODE == "demo" else {
        "title": "Trade signals",
        "stages": [{"label": "Scanned", "value": 40, "atRisk": False},
                   {"label": "Setups", "value": 6, "atRisk": False},
                   {"label": "Triggered", "value": 1, "atRisk": True}],
    }
    spoken = f"{data['stages'][-1]['value']} signal triggered out of {data['stages'][0]['value']} scanned."
    return spoken, _event("pipeline", data)


def research_intel(query: str = "") -> Tuple[str, dict]:
    if DATA_MODE != "demo":
        try:
            r = httpx.post(f"{BACKEND}/api/ask", json={"question": query or "what should I know today", "topK": 4}, timeout=60)
            answer = r.json().get("answer", "")
            items = [{"when": "now", "text": answer}]
        except Exception:  # noqa: BLE001
            items = [{"when": "now", "text": "Backend unavailable."}]
    else:
        items = _seed("intel.json").get("items", [])
    spoken = items[0]["text"] if items else "Nothing to report."
    return spoken, _event("intel", {"title": "Intel", "items": items})


def plan_my_day() -> Tuple[str, dict]:
    data = _seed("actions.json") if DATA_MODE == "demo" else {
        "title": "Today", "items": [{"text": "Review overnight PRs", "done": False}]}
    n = len(data.get("items", []))
    spoken = f"You have {n} thing{'s' if n != 1 else ''} planned today."
    return spoken, _event("actions", data)


TOOLS = {
    "get_daily_brief": get_daily_brief,
    "show_metrics": show_metrics,
    "get_pipeline": get_pipeline,
    "research_intel": research_intel,
    "plan_my_day": plan_my_day,
}
