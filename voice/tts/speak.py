"""Piper TTS (local, CPU) — Section 11.6.

Synthesizes a sentence to 16-bit PCM samples for immediate playback. Voice model is chosen by the
active profile (en_US / hi_IN / mr_IN …). Runs on CPU so it doesn't compete with the GPU LLM.
"""
from __future__ import annotations

import os
import wave
from io import BytesIO
from pathlib import Path
from typing import Optional

import numpy as np

try:
    from piper import PiperVoice

    _AVAILABLE = True
except Exception:  # noqa: BLE001
    _AVAILABLE = False

VOICE_DIR = Path(os.getenv("ULTRON_PIPER_DIR", "language/language_packs"))
DEFAULT_VOICE = os.getenv("ULTRON_PIPER_VOICE", "en_US-amy-medium.onnx")


class PiperEngine:
    def __init__(self, voice_file: str = DEFAULT_VOICE) -> None:
        self._voice = None
        self.sample_rate = 22050
        path = VOICE_DIR / voice_file
        if _AVAILABLE and path.exists():
            self._voice = PiperVoice.load(str(path))
            self.sample_rate = self._voice.config.sample_rate

    def available(self) -> bool:
        return self._voice is not None

    def synthesize(self, text: str) -> Optional[np.ndarray]:
        """Return int16 mono PCM for `text`, or None if Piper isn't available."""
        if self._voice is None or not text.strip():
            return None
        buf = BytesIO()
        with wave.open(buf, "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)
            wav_file.setframerate(self.sample_rate)
            self._voice.synthesize(text, wav_file)
        buf.seek(0)
        with wave.open(buf, "rb") as wav_file:
            frames = wav_file.readframes(wav_file.getnframes())
        return np.frombuffer(frames, dtype=np.int16)


def play(pcm_int16: np.ndarray, sample_rate: int) -> None:
    """Blocking playback via sounddevice (used by the standalone pipeline)."""
    try:
        import sounddevice as sd

        sd.play(pcm_int16, samplerate=sample_rate)
        sd.wait()
    except Exception:  # noqa: BLE001
        pass
