"""Risk management (Section 7). Kelly criterion + fixed-fractional position sizing + R:R. Pure
Python, mirrors the Java RiskCalculator so both layers agree on sizing math.
"""
from __future__ import annotations

import math

KELLY_CAP = 0.25


def position_size(account_value: float, risk_fraction: float, entry: float, stop: float) -> int:
    per_unit = abs(entry - stop)
    if per_unit <= 0 or account_value <= 0 or risk_fraction <= 0:
        return 0
    return math.floor((account_value * risk_fraction) / per_unit)


def kelly_fraction(win_rate: float, win_loss_ratio: float) -> float:
    if win_loss_ratio <= 0:
        return 0.0
    f = win_rate - (1.0 - win_rate) / win_loss_ratio
    return max(0.0, min(KELLY_CAP, f))


def risk_reward(entry: float, stop: float, target: float) -> float:
    risk = abs(entry - stop)
    return 0.0 if risk <= 0 else abs(target - entry) / risk


def recommend(account_value: float, risk_fraction: float, entry: float, stop: float, target: float) -> dict:
    qty = position_size(account_value, risk_fraction, entry, stop)
    rr = risk_reward(entry, stop, target)
    return {
        "quantity": qty,
        "riskReward": round(rr, 4),
        "maxLoss": round(qty * abs(entry - stop), 2),
        "maxGain": round(qty * abs(target - entry), 2),
    }
