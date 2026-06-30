"""First-pass spoken-language detection (Section 9.10.B).

Runs on the first ~1-2s of audio so the right STT language is selected before full transcription,
rather than mis-transcribing then re-doing it. Uses faster-whisper's built-in language id.
"""
from __future__ import annotations

import os
from typing import Optional

import numpy as np

try:
    from faster_whisper import WhisperModel

    _MODEL: Optional[WhisperModel] = None

    def _model() -> "WhisperModel":
        global _MODEL
        if _MODEL is None:
            _MODEL = WhisperModel(
                os.getenv("ULTRON_WHISPER_MODEL", "base"),
                device=os.getenv("ULTRON_WHISPER_DEVICE", "cpu"),
                compute_type=os.getenv("ULTRON_WHISPER_COMPUTE", "int8"),
            )
        return _MODEL

    _AVAILABLE = True
except Exception:  # noqa: BLE001
    _AVAILABLE = False

# Launch languages (Section 9.10.C). Detection falls back to English when ambiguous.
SUPPORTED = {"en", "hi", "mr"}


def detect_language(audio_f32: np.ndarray) -> str:
    """Return an ISO-639-1 code for the dominant language in a short audio clip."""
    if not _AVAILABLE or audio_f32 is None or len(audio_f32) == 0:
        return "en"
    try:
        # transcribe with a tiny window just to read info.language
        _segments, info = _model().transcribe(audio_f32, beam_size=1, vad_filter=True)
        lang = info.language
        return lang if lang in SUPPORTED else (lang or "en")
    except Exception:  # noqa: BLE001
        return "en"
