"""Behaviour profiling (L7).

Aggregates observed events into simple, explainable stats (counts per intent, busiest hour). The
heavy version (Prophet forecasting + K-means clustering) lives behind the same ``profile`` surface.
"""
from __future__ import annotations

from collections import Counter
from typing import Dict, List


def profile(events: List[dict]) -> dict:
    """Summarise a list of events shaped like {"intent": str, "hour": int}."""
    if not events:
        return {"count": 0, "by_intent": {}, "busiest_hour": None, "top_intent": None}

    intents: Counter = Counter()
    hours: Counter = Counter()
    for ev in events:
        intent = str(ev.get("intent", "UNKNOWN"))
        intents[intent] += 1
        hour = ev.get("hour")
        if isinstance(hour, int) and 0 <= hour <= 23:
            hours[hour] += 1

    by_intent: Dict[str, int] = dict(intents)
    busiest_hour = hours.most_common(1)[0][0] if hours else None
    top_intent = intents.most_common(1)[0][0] if intents else None
    return {
        "count": len(events),
        "by_intent": by_intent,
        "busiest_hour": busiest_hour,
        "top_intent": top_intent,
    }
