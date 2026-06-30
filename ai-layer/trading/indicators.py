"""Technical indicators (Section 7). Wraps TA-Lib when installed; otherwise computes the core
indicators in pure NumPy so the trading layer works without the (notoriously fiddly) TA-Lib C
dependency. Same function signatures either way.
"""
from __future__ import annotations

from typing import Sequence

import numpy as np

try:
    import talib  # type: ignore

    _TALIB = True
except Exception:  # noqa: BLE001
    _TALIB = False


def _arr(x: Sequence[float]) -> np.ndarray:
    return np.asarray(x, dtype="float64")


def rsi(close: Sequence[float], period: int = 14) -> float:
    c = _arr(close)
    if _TALIB and len(c) > period:
        return float(talib.RSI(c, timeperiod=period)[-1])
    if len(c) <= period:
        return 50.0
    delta = np.diff(c)
    gain = np.where(delta > 0, delta, 0.0)
    loss = np.where(delta < 0, -delta, 0.0)
    avg_gain = np.mean(gain[-period:])
    avg_loss = np.mean(loss[-period:])
    if avg_loss == 0:
        return 100.0
    rs = avg_gain / avg_loss
    return float(100.0 - (100.0 / (1.0 + rs)))


def macd(close: Sequence[float], fast: int = 12, slow: int = 26, signal: int = 9) -> dict:
    c = _arr(close)
    if _TALIB and len(c) > slow:
        macd_line, sig_line, hist = talib.MACD(c, fast, slow, signal)
        return {"macd": float(macd_line[-1]), "macdSignal": float(sig_line[-1]), "hist": float(hist[-1])}

    def ema(arr: np.ndarray, span: int) -> np.ndarray:
        alpha = 2.0 / (span + 1)
        out = np.zeros_like(arr)
        out[0] = arr[0]
        for i in range(1, len(arr)):
            out[i] = alpha * arr[i] + (1 - alpha) * out[i - 1]
        return out

    if len(c) < slow:
        return {"macd": 0.0, "macdSignal": 0.0, "hist": 0.0}
    macd_line = ema(c, fast) - ema(c, slow)
    sig_line = ema(macd_line, signal)
    return {"macd": float(macd_line[-1]), "macdSignal": float(sig_line[-1]),
            "hist": float(macd_line[-1] - sig_line[-1])}


def bollinger(close: Sequence[float], period: int = 20, num_std: float = 2.0) -> dict:
    c = _arr(close)
    if len(c) < period:
        last = float(c[-1]) if len(c) else 0.0
        return {"upper": last, "middle": last, "lower": last}
    window = c[-period:]
    mid = float(np.mean(window))
    sd = float(np.std(window))
    return {"upper": mid + num_std * sd, "middle": mid, "lower": mid - num_std * sd}


def atr(high: Sequence[float], low: Sequence[float], close: Sequence[float], period: int = 14) -> float:
    h, l, c = _arr(high), _arr(low), _arr(close)
    if _TALIB and len(c) > period:
        return float(talib.ATR(h, l, c, timeperiod=period)[-1])
    if len(c) < 2:
        return 0.0
    tr = np.maximum(h[1:] - l[1:], np.maximum(np.abs(h[1:] - c[:-1]), np.abs(l[1:] - c[:-1])))
    return float(np.mean(tr[-period:])) if len(tr) >= period else float(np.mean(tr))


def vwap(high: Sequence[float], low: Sequence[float], close: Sequence[float],
         volume: Sequence[float]) -> float:
    h, l, c, v = _arr(high), _arr(low), _arr(close), _arr(volume)
    typical = (h + l + c) / 3.0
    denom = np.sum(v)
    return float(np.sum(typical * v) / denom) if denom > 0 else float(c[-1])


def snapshot(high, low, close, volume=None) -> dict:
    """Compute the indicator snapshot the Java SignalGenerator expects."""
    m = macd(close)
    return {
        "rsi": rsi(close),
        "macd": m["macd"],
        "macdSignal": m["macdSignal"],
        "atr": atr(high, low, close),
        "bollinger": bollinger(close),
        "backend": "talib" if _TALIB else "numpy",
    }
