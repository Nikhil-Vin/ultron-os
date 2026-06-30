"""Intent classification (L7). Stdlib heuristic mirroring the Java IntentClassifier.

Upgradeable to a HuggingFace + PEFT/LoRA model behind the same ``classify`` surface.
"""
from __future__ import annotations

INTENTS = ("CAPTURE", "PLAN", "TRADE_WATCH", "TRADE_LIVE", "QUESTION", "SMALL_TALK")

_GREETINGS = ("hi", "hello", "hey", "good morning", "good evening", "how are you", "thanks", "thank you")


def classify(text: str) -> str:
    if not text or not text.strip():
        return "QUESTION"
    t = text.lower().strip()

    if (
        any(k in t for k in ("live trade", "live-trade", "place an order", "execute trade"))
        or t.startswith("buy ")
        or t.startswith("sell ")
        or " buy " in t
        or " sell " in t
    ):
        return "TRADE_LIVE"
    if any(k in t for k in ("watchlist", "market", "watch the", "stock price", "ticker")):
        return "TRADE_WATCH"
    if any(k in t for k in ("remember", "note that", "make a note", "capture", "save this", "log that")):
        return "CAPTURE"
    if any(k in t for k in ("plan ", "plan my", "prioriti", "schedule my", "organise", "organize", "to-do", "todo")):
        return "PLAN"
    if any(t == g or t.startswith(g + " ") or t.startswith(g + ",") for g in _GREETINGS):
        return "SMALL_TALK"
    return "QUESTION"
