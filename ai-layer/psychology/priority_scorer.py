"""Priority scoring (L7). Stdlib heuristic mirroring the Java PriorityScorer.

Upgradeable to an XGBoost model behind the same ``score`` surface.
"""
from __future__ import annotations

_URGENT = ("urgent", "asap", "immediately", "now", "critical", "emergency", "today", "deadline")
_HIGH = ("important", "must", "blocker", "blocked", "overdue", "high priority")
_LOW = ("someday", "maybe", "whenever", "low priority", "nice to have", "fyi")


def score(text: str) -> float:
    if not text or not text.strip():
        return 0.0
    t = text.lower()
    s = 0.3
    s += 0.25 * sum(1 for w in _URGENT if w in t)
    s += 0.15 * sum(1 for w in _HIGH if w in t)
    s -= 0.20 * sum(1 for w in _LOW if w in t)
    if "!" in t:
        s += 0.05
    return max(0.0, min(1.0, s))
