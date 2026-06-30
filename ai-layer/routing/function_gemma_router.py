"""Function Gemma router (Section 11.3 / Phase 5 local-optimization track).

A small fast model decides whether a prompt needs the full "thinking" LLM or a quick direct/tool
call — keeping VRAM low (~6GB target) on consumer hardware. Uses a fine-tuned Function Gemma (270M)
when available; otherwise a deterministic heuristic so routing always works offline.

Returns one of: {"route": "think"|"direct"|"tool", "tool": <name|None>, "confidence": float}.
"""
from __future__ import annotations

import os
import re
from typing import Optional

MODEL_PATH = os.getenv("ULTRON_ROUTER_MODEL", "models/function_gemma_router")

try:
    from transformers import AutoModelForCausalLM, AutoTokenizer  # type: ignore
    import torch  # type: ignore

    _tok = None
    _model = None

    def _ensure():
        global _tok, _model
        if _model is None:
            _tok = AutoTokenizer.from_pretrained(MODEL_PATH)
            _model = AutoModelForCausalLM.from_pretrained(MODEL_PATH)
        return _tok, _model

    _AVAILABLE = os.path.isdir(MODEL_PATH)
except Exception:  # noqa: BLE001
    _AVAILABLE = False

# Tool keywords for the heuristic direct-route.
_TOOL_HINTS = {
    "trading_signal": ("signal", "buy", "sell", "rsi", "macd", "setup"),
    "capture_memory": ("remember", "note", "save this", "log that"),
    "ask": ("what", "who", "when", "where", "how", "explain", "summarise", "summarize"),
}
_THINK_HINTS = ("plan", "analyse", "analyze", "strategy", "compare", "design", "reason", "why", "trade-off")


def route(prompt: str) -> dict:
    if _AVAILABLE:
        try:
            return _route_model(prompt)
        except Exception:  # noqa: BLE001
            pass
    return _route_heuristic(prompt)


def _route_model(prompt: str) -> dict:
    tok, model = _ensure()
    instruction = (
        "Classify the request as THINK (needs deep reasoning), DIRECT (simple answer), or "
        "TOOL:<name>. Request: " + prompt + "\nLabel:"
    )
    inputs = tok(instruction, return_tensors="pt")
    out = model.generate(**inputs, max_new_tokens=8, do_sample=False)
    label = tok.decode(out[0][inputs["input_ids"].shape[1]:], skip_special_tokens=True).strip().upper()
    if label.startswith("TOOL:"):
        return {"route": "tool", "tool": label.split(":", 1)[1].strip().lower(), "confidence": 0.9}
    if label.startswith("THINK"):
        return {"route": "think", "tool": None, "confidence": 0.9}
    return {"route": "direct", "tool": None, "confidence": 0.8}


def _route_heuristic(prompt: str) -> dict:
    p = (prompt or "").lower()
    for tool, hints in _TOOL_HINTS.items():
        if any(h in p for h in hints):
            # Questions go through the LLM as DIRECT/ask; trading + capture are clear tool routes.
            if tool == "ask":
                break
            return {"route": "tool", "tool": tool, "confidence": 0.7}
    if any(h in p for h in _THINK_HINTS) or len(p.split()) > 40:
        return {"route": "think", "tool": None, "confidence": 0.65}
    return {"route": "direct", "tool": None, "confidence": 0.6}


def available() -> bool:
    return _AVAILABLE
