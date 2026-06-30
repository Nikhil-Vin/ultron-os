"""Finance news sentiment (Section 7). FinBERT when transformers is installed; otherwise a finance
lexicon fallback so the signal engine always has a sentiment input. Returns score in [-1, 1].
"""
from __future__ import annotations

import re
from typing import List

try:
    from transformers import pipeline  # type: ignore

    _PIPE = None

    def _finbert():
        global _PIPE
        if _PIPE is None:
            _PIPE = pipeline("sentiment-analysis", model="ProsusAI/finbert")
        return _PIPE

    _AVAILABLE = True
except Exception:  # noqa: BLE001
    _AVAILABLE = False

_POS = {"beat", "beats", "surge", "rally", "growth", "profit", "upgrade", "bullish", "record",
        "gain", "gains", "strong", "outperform", "buy"}
_NEG = {"miss", "misses", "plunge", "fall", "loss", "downgrade", "bearish", "weak", "fraud",
        "selloff", "cut", "warning", "underperform", "sell", "crash"}
_WORD = re.compile(r"[a-z']+")


def score_text(text: str) -> float:
    if not text:
        return 0.0
    if _AVAILABLE:
        try:
            r = _finbert()(text[:512])[0]
            label = r["label"].lower()
            s = float(r["score"])
            if label == "positive":
                return s
            if label == "negative":
                return -s
            return 0.0
        except Exception:  # noqa: BLE001
            pass
    tokens = _WORD.findall(text.lower())
    pos = sum(1 for t in tokens if t in _POS)
    neg = sum(1 for t in tokens if t in _NEG)
    total = pos + neg
    return 0.0 if total == 0 else (pos - neg) / total


def score_headlines(headlines: List[str]) -> dict:
    if not headlines:
        return {"score": 0.0, "backend": "finbert" if _AVAILABLE else "lexicon", "n": 0}
    scores = [score_text(h) for h in headlines]
    return {
        "score": round(sum(scores) / len(scores), 4),
        "backend": "finbert" if _AVAILABLE else "lexicon",
        "n": len(scores),
    }
