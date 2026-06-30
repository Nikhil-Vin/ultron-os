"""Candlestick pattern recognition (Section 7).

When PyTorch is installed, defines a small CNN over OHLCV "images" (and loads trained weights if
present). Without torch, falls back to classic rule-based candlestick detection (doji, hammer,
engulfing, ...) over raw OHLC so pattern signals are always available.
"""
from __future__ import annotations

from typing import List, Sequence

try:
    import torch  # type: ignore
    import torch.nn as nn  # type: ignore

    class CandleCNN(nn.Module):
        """Small CNN: OHLCV window rendered to a 1x64x64 image → pattern logits."""

        def __init__(self, num_classes: int = 6):
            super().__init__()
            self.net = nn.Sequential(
                nn.Conv2d(1, 16, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
                nn.Conv2d(16, 32, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
                nn.Flatten(),
                nn.Linear(32 * 16 * 16, 64), nn.ReLU(),
                nn.Linear(64, num_classes),
            )

        def forward(self, x):
            return self.net(x)

    _TORCH = True
except Exception:  # noqa: BLE001
    _TORCH = False

PATTERNS = ["none", "doji", "hammer", "shooting_star", "bullish_engulfing", "bearish_engulfing"]


def _rule_based(o: Sequence[float], h: Sequence[float], low: Sequence[float], c: Sequence[float]) -> dict:
    """Detect a pattern on the latest 1-2 candles with classic rules."""
    if len(c) < 2:
        return {"pattern": "none", "confidence": 0.0, "backend": "rules"}
    o1, h1, l1, c1 = o[-1], h[-1], low[-1], c[-1]
    o0, c0 = o[-2], c[-2]
    body = abs(c1 - o1)
    rng = max(h1 - l1, 1e-9)
    upper = h1 - max(o1, c1)
    lower = min(o1, c1) - l1

    if body / rng < 0.1:
        return {"pattern": "doji", "confidence": 0.7, "backend": "rules"}
    if lower > body * 2 and upper < body:
        return {"pattern": "hammer", "confidence": 0.65, "backend": "rules"}
    if upper > body * 2 and lower < body:
        return {"pattern": "shooting_star", "confidence": 0.65, "backend": "rules"}
    if c1 > o1 and c0 < o0 and c1 >= o0 and o1 <= c0:
        return {"pattern": "bullish_engulfing", "confidence": 0.75, "backend": "rules"}
    if c1 < o1 and c0 > o0 and o1 >= c0 and c1 <= o0:
        return {"pattern": "bearish_engulfing", "confidence": 0.75, "backend": "rules"}
    return {"pattern": "none", "confidence": 0.3, "backend": "rules"}


def detect(open_: List[float], high: List[float], low: List[float], close: List[float],
           weights_path: str | None = None) -> dict:
    # Rule-based is the robust default; the CNN is an optional accuracy upgrade with trained weights.
    if _TORCH and weights_path:
        try:
            model = CandleCNN()
            model.load_state_dict(torch.load(weights_path, map_location="cpu"))
            model.eval()
            img = _render(open_, high, low, close)
            with torch.no_grad():
                logits = model(img)
                idx = int(torch.argmax(logits, dim=1).item())
                conf = float(torch.softmax(logits, dim=1)[0, idx].item())
            return {"pattern": PATTERNS[idx], "confidence": round(conf, 3), "backend": "cnn"}
        except Exception:  # noqa: BLE001
            pass
    return _rule_based(open_, high, low, close)


def _render(o, h, low, c):
    """Render an OHLC window into a 1x64x64 tensor (normalised). Torch-only."""
    import numpy as np  # type: ignore
    n = min(len(c), 64)
    grid = np.zeros((64, 64), dtype="float32")
    series = np.array(c[-n:], dtype="float32")
    lo, hi = series.min(), series.max()
    rng = max(hi - lo, 1e-9)
    for x in range(n):
        y = int((series[x] - lo) / rng * 63)
        grid[63 - y, x] = 1.0
    return torch.from_numpy(grid).unsqueeze(0).unsqueeze(0)
