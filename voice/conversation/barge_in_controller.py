"""Barge-in + end-of-turn detection via Silero VAD (Section 9.9.A).

Two jobs:
  1. END-OF-TURN: while listening, detect when the owner stops speaking (silence > threshold).
  2. BARGE-IN: while Ultron is speaking, an always-on VAD that fires the moment the owner starts
     talking, so playback + the in-flight LLM stream can be cancelled within ~150ms.
"""
from __future__ import annotations

import os
from collections import deque
from typing import Optional

import numpy as np

try:
    from silero_vad import load_silero_vad, VADIterator

    _AVAILABLE = True
except Exception:  # noqa: BLE001
    _AVAILABLE = False

SAMPLE_RATE = 16000
SILENCE_MS = int(os.getenv("ULTRON_EOT_SILENCE_MS", "250"))   # end-of-turn after this much silence
SPEECH_PROB = float(os.getenv("ULTRON_VAD_THRESHOLD", "0.5"))


class VadController:
    def __init__(self) -> None:
        self._model = None
        self._iter: Optional[object] = None
        if _AVAILABLE:
            self._model = load_silero_vad()
            self._iter = VADIterator(self._model, threshold=SPEECH_PROB, sampling_rate=SAMPLE_RATE)
        self._recent = deque(maxlen=10)

    def available(self) -> bool:
        return self._model is not None

    def is_speech(self, frame_f32: np.ndarray) -> bool:
        """True if the 30ms frame contains speech. Used for barge-in (always-on during playback)."""
        if self._model is None:
            return False
        try:
            import torch

            prob = float(self._model(torch.from_numpy(frame_f32), SAMPLE_RATE).item())
        except Exception:  # noqa: BLE001
            return False
        self._recent.append(prob)
        return prob >= SPEECH_PROB

    def reset(self) -> None:
        if self._iter is not None and hasattr(self._iter, "reset_states"):
            self._iter.reset_states()
