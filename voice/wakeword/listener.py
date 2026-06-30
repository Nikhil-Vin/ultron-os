"""Wake-word detection via openWakeWord (Section 11.6). Target latency < 200ms.

Streams 16kHz mono frames and fires when the "Ultron" model score crosses the threshold. Ships
with a graceful availability flag so the module imports before the dependency is installed.
"""
from __future__ import annotations

import os
from typing import Optional

import numpy as np

try:
    from openwakeword.model import Model

    _AVAILABLE = True
except Exception:  # noqa: BLE001
    _AVAILABLE = False

# A custom "ultron" .onnx/.tflite model trained via openWakeWord, or a bundled keyword.
WAKE_MODEL_PATH = os.getenv("ULTRON_WAKE_MODEL", "models/ultron.onnx")
WAKE_THRESHOLD = float(os.getenv("ULTRON_WAKE_THRESHOLD", "0.5"))


class WakeWordListener:
    def __init__(self, model_path: str = WAKE_MODEL_PATH, threshold: float = WAKE_THRESHOLD) -> None:
        self.threshold = threshold
        self._model: Optional[object] = None
        if _AVAILABLE:
            try:
                self._model = Model(wakeword_models=[model_path]) if os.path.exists(model_path) else Model()
            except Exception:  # noqa: BLE001
                self._model = Model()  # default bundled models

    def available(self) -> bool:
        return self._model is not None

    def detect(self, frame_int16: np.ndarray) -> bool:
        """Feed a frame (int16 mono 16kHz) and return True if the wake word fired this frame."""
        if self._model is None:
            return False
        scores = self._model.predict(frame_int16)
        return any(score >= self.threshold for score in scores.values())

    def reset(self) -> None:
        if self._model is not None and hasattr(self._model, "reset"):
            self._model.reset()
