"""Online feedback loop (L7). Stdlib EMA per-intent confidence, mirroring the Java FeedbackLoop.

The production version uses River for true online learning; the contract (record / weight) is stable.
"""
from __future__ import annotations

from typing import Dict

ALPHA = 0.2
START = 0.5


class FeedbackLoop:
    def __init__(self) -> None:
        self._weights: Dict[str, float] = {}

    def record(self, intent: str, positive: bool) -> None:
        if not intent:
            return
        target = 1.0 if positive else 0.0
        current = self._weights.get(intent, START)
        self._weights[intent] = ALPHA * target + (1 - ALPHA) * current

    def weight(self, intent: str) -> float:
        return self._weights.get(intent, START)

    def snapshot(self) -> Dict[str, float]:
        return dict(self._weights)
