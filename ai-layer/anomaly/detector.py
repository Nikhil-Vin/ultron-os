"""Anomaly detection.

Stdlib statistical outlier detection (modified z-score, robust to small samples) that always works;
upgrades to scikit-learn's IsolationForest when installed.
"""
from __future__ import annotations

import statistics
from typing import List


def detect(series: List[float], z_threshold: float = 3.5) -> dict:
    """Flag anomalies in a 1-D numeric series. Returns indices + values of outliers."""
    values = [float(x) for x in (series or [])]
    if len(values) < 3:
        return {"backend": "insufficient-data", "anomalies": [], "count": len(values)}

    try:
        import numpy as np  # type: ignore
        from sklearn.ensemble import IsolationForest  # type: ignore

        arr = np.asarray(values, dtype="float64").reshape(-1, 1)
        model = IsolationForest(random_state=0, contamination="auto")
        preds = model.fit_predict(arr)
        anomalies = [
            {"index": i, "value": values[i]} for i, p in enumerate(preds) if p == -1
        ]
        return {"backend": "isolation-forest", "anomalies": anomalies, "count": len(values)}
    except Exception:  # noqa: BLE001 - stdlib modified z-score fallback
        pass

    median = statistics.median(values)
    abs_dev = [abs(x - median) for x in values]
    mad = statistics.median(abs_dev)
    anomalies = []
    if mad == 0:
        mean = statistics.fmean(values)
        stdev = statistics.pstdev(values) or 1.0
        for i, x in enumerate(values):
            if abs(x - mean) / stdev >= z_threshold:
                anomalies.append({"index": i, "value": x})
    else:
        for i, x in enumerate(values):
            modified_z = 0.6745 * (x - median) / mad
            if abs(modified_z) >= z_threshold:
                anomalies.append({"index": i, "value": x})
    return {"backend": "modified-zscore", "anomalies": anomalies, "count": len(values)}
