"""Signal engine (Section 7). Fuses the indicator snapshot + news sentiment into a structured
BUY/SELL/HOLD with confidence and reasons. Mirrors the Java SignalGenerator so the bridge can
override the JVM heuristic with this richer (TA-Lib/FinBERT-backed) version.
"""
from __future__ import annotations

from typing import List

from trading import indicators, sentiment


def generate(instrument: str, high, low, close, volume=None, headlines: List[str] | None = None) -> dict:
    ind = indicators.snapshot(high, low, close, volume)
    sent = sentiment.score_headlines(headlines or [])

    score = 0.0
    reasons: List[str] = []

    rsi = ind["rsi"]
    if rsi < 30:
        score += 0.4
        reasons.append(f"RSI {rsi:.1f} oversold")
    elif rsi > 70:
        score -= 0.4
        reasons.append(f"RSI {rsi:.1f} overbought")

    if ind["macd"] > ind["macdSignal"]:
        score += 0.3
        reasons.append("MACD bullish crossover")
    elif ind["macd"] < ind["macdSignal"]:
        score -= 0.3
        reasons.append("MACD bearish crossover")

    s = sent["score"]
    if s > 0.2:
        score += 0.2
        reasons.append("positive news sentiment")
    elif s < -0.2:
        score -= 0.2
        reasons.append("negative news sentiment")

    if score >= 0.3:
        signal = "BUY"
    elif score <= -0.3:
        signal = "SELL"
    else:
        signal = "HOLD"

    return {
        "instrument": instrument,
        "signal": signal,
        "confidence": round(min(1.0, abs(score)), 4),
        "reasoning": "; ".join(reasons),
        "indicators": ind,
        "sentiment": sent,
    }
