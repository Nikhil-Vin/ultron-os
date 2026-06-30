"""Backtesting (Section 7). Uses VectorBT when installed; otherwise a self-contained vectorized
SMA-crossover backtest in NumPy so strategy ideas can be validated with zero heavy deps. Reports
the PyFolio-style basics (return, win rate, max drawdown, Sharpe).
"""
from __future__ import annotations

from typing import Sequence

import numpy as np


def sma_crossover(close: Sequence[float], fast: int = 10, slow: int = 30) -> dict:
    c = np.asarray(close, dtype="float64")
    if len(c) < slow + 2:
        return {"error": "not enough data", "bars": len(c)}

    try:
        import vectorbt as vbt  # type: ignore

        fast_ma = vbt.MA.run(c, fast)
        slow_ma = vbt.MA.run(c, slow)
        entries = fast_ma.ma_crossed_above(slow_ma)
        exits = fast_ma.ma_crossed_below(slow_ma)
        pf = vbt.Portfolio.from_signals(c, entries, exits, init_cash=100_000)
        return {
            "backend": "vectorbt",
            "total_return_pct": round(float(pf.total_return()) * 100, 2),
            "sharpe": round(float(pf.sharpe_ratio()), 3),
            "max_drawdown_pct": round(float(pf.max_drawdown()) * 100, 2),
            "trades": int(pf.trades.count()),
        }
    except Exception:  # noqa: BLE001 - numpy fallback
        pass

    def sma(a: np.ndarray, n: int) -> np.ndarray:
        out = np.full_like(a, np.nan)
        cs = np.cumsum(np.insert(a, 0, 0))
        out[n - 1:] = (cs[n:] - cs[:-n]) / n
        return out

    fast_ma = sma(c, fast)
    slow_ma = sma(c, slow)
    pos = np.where(fast_ma > slow_ma, 1.0, 0.0)
    pos = np.nan_to_num(pos)
    rets = np.diff(c) / c[:-1]
    strat = pos[:-1] * rets
    equity = np.cumprod(1 + strat)
    total_return = float(equity[-1] - 1.0) if len(equity) else 0.0
    # crossings = trade count
    trades = int(np.sum(np.abs(np.diff(pos))))
    peak = np.maximum.accumulate(equity) if len(equity) else np.array([1.0])
    dd = float(np.min(equity / peak - 1.0)) if len(equity) else 0.0
    sharpe = float(np.mean(strat) / (np.std(strat) + 1e-9) * np.sqrt(252)) if len(strat) else 0.0
    return {
        "backend": "numpy",
        "total_return_pct": round(total_return * 100, 2),
        "sharpe": round(sharpe, 3),
        "max_drawdown_pct": round(dd * 100, 2),
        "trades": trades,
    }
