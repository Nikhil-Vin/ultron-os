"""Options Greeks (Section 7). Black-Scholes price + Delta/Gamma/Theta/Vega/Rho. Uses py_vollib /
mibian when installed; otherwise a self-contained Black-Scholes implementation in pure math so the
greeks are always available offline.
"""
from __future__ import annotations

import math


def _norm_cdf(x: float) -> float:
    return 0.5 * (1 + math.erf(x / math.sqrt(2)))


def _norm_pdf(x: float) -> float:
    return math.exp(-0.5 * x * x) / math.sqrt(2 * math.pi)


def greeks(spot: float, strike: float, t_years: float, rate: float, vol: float,
           kind: str = "call") -> dict:
    """Black-Scholes price + greeks. t_years in years, rate/vol as decimals (e.g. 0.06, 0.18)."""
    if t_years <= 0 or vol <= 0 or spot <= 0 or strike <= 0:
        return {"error": "inputs must be positive (and time/vol > 0)"}

    d1 = (math.log(spot / strike) + (rate + 0.5 * vol * vol) * t_years) / (vol * math.sqrt(t_years))
    d2 = d1 - vol * math.sqrt(t_years)
    call = kind.lower() == "call"

    if call:
        price = spot * _norm_cdf(d1) - strike * math.exp(-rate * t_years) * _norm_cdf(d2)
        delta = _norm_cdf(d1)
        theta = (-(spot * _norm_pdf(d1) * vol) / (2 * math.sqrt(t_years))
                 - rate * strike * math.exp(-rate * t_years) * _norm_cdf(d2))
        rho = strike * t_years * math.exp(-rate * t_years) * _norm_cdf(d2)
    else:
        price = strike * math.exp(-rate * t_years) * _norm_cdf(-d2) - spot * _norm_cdf(-d1)
        delta = _norm_cdf(d1) - 1
        theta = (-(spot * _norm_pdf(d1) * vol) / (2 * math.sqrt(t_years))
                 + rate * strike * math.exp(-rate * t_years) * _norm_cdf(-d2))
        rho = -strike * t_years * math.exp(-rate * t_years) * _norm_cdf(-d2)

    gamma = _norm_pdf(d1) / (spot * vol * math.sqrt(t_years))
    vega = spot * _norm_pdf(d1) * math.sqrt(t_years)

    return {
        "price": round(price, 4),
        "delta": round(delta, 4),
        "gamma": round(gamma, 6),
        "theta_per_day": round(theta / 365.0, 4),
        "vega_per_1pct": round(vega / 100.0, 4),
        "rho_per_1pct": round(rho / 100.0, 4),
        "backend": "black-scholes",
    }
