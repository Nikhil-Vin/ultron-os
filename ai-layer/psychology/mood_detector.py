"""Mood / sentiment detection (L7).

Stdlib lexicon-based polarity in [-1, 1] with a mood label; upgrades to TextBlob when installed.
Speech-based mood (SpeechBrain) is a Phase 2 audio concern and intentionally out of scope here.
"""
from __future__ import annotations

import re

_POSITIVE = {
    "good", "great", "excellent", "happy", "glad", "love", "awesome", "win", "success",
    "calm", "confident", "thanks", "nice", "progress", "done", "fixed",
}
_NEGATIVE = {
    "bad", "terrible", "sad", "angry", "hate", "fail", "failure", "broken", "bug", "stuck",
    "frustrated", "anxious", "worried", "panic", "stress", "stressed", "overwhelmed", "fomo",
}

_TOKEN_RE = re.compile(r"[a-z']+")


def detect(text: str) -> dict:
    try:
        from textblob import TextBlob  # type: ignore

        polarity = float(TextBlob(text or "").sentiment.polarity)
        return {"backend": "textblob", "polarity": round(polarity, 3), "mood": _label(polarity)}
    except Exception:  # noqa: BLE001 - offline lexicon fallback
        pass

    tokens = _TOKEN_RE.findall((text or "").lower())
    if not tokens:
        return {"backend": "lexicon", "polarity": 0.0, "mood": "neutral"}
    pos = sum(1 for t in tokens if t in _POSITIVE)
    neg = sum(1 for t in tokens if t in _NEGATIVE)
    total = pos + neg
    polarity = 0.0 if total == 0 else (pos - neg) / total
    return {"backend": "lexicon", "polarity": round(polarity, 3), "mood": _label(polarity)}


def _label(polarity: float) -> str:
    if polarity > 0.25:
        return "positive"
    if polarity < -0.25:
        return "negative"
    return "neutral"
