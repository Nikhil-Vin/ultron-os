"""Streaming speech-to-text via faster-whisper (Section 11.6).

Multilingual checkpoint natively covers English/Hindi/Marathi + 90 more. Emits partial transcripts
as audio accumulates and a final transcript at end-of-turn. Per-segment language is returned to
support code-switching (Section 9.10).
"""
from __future__ import annotations

import os
from typing import Iterator, List, Optional

import numpy as np

try:
    from faster_whisper import WhisperModel

    _AVAILABLE = True
except Exception:  # noqa: BLE001
    _AVAILABLE = False

MODEL_SIZE = os.getenv("ULTRON_WHISPER_MODEL", "base")
DEVICE = os.getenv("ULTRON_WHISPER_DEVICE", "cpu")
COMPUTE = os.getenv("ULTRON_WHISPER_COMPUTE", "int8")
SAMPLE_RATE = 16000


class StreamingTranscriber:
    def __init__(self, model_size: str = MODEL_SIZE) -> None:
        self._model = None
        if _AVAILABLE:
            self._model = WhisperModel(model_size, device=DEVICE, compute_type=COMPUTE)

    def available(self) -> bool:
        return self._model is not None

    def transcribe(self, audio_f32: np.ndarray, language: Optional[str] = None) -> dict:
        """Transcribe a complete utterance (float32 mono 16kHz in [-1,1]).

        Returns {text, language, segments:[{text,lang}]}.
        """
        if self._model is None:
            return {"text": "", "language": language or "en", "segments": []}
        segments, info = self._model.transcribe(
            audio_f32,
            language=language,            # None → auto-detect (first-pass detector may set this)
            vad_filter=True,
            beam_size=1,                  # greedy for low latency
        )
        seg_list: List[dict] = []
        text_parts: List[str] = []
        for seg in segments:
            text_parts.append(seg.text)
            seg_list.append({"text": seg.text.strip(), "lang": info.language})
        return {
            "text": " ".join(t.strip() for t in text_parts).strip(),
            "language": info.language,
            "segments": seg_list,
        }

    def stream(self, audio_chunks: Iterator[np.ndarray], language: Optional[str] = None) -> Iterator[dict]:
        """Yield growing partial transcripts as chunks arrive, then a final transcript."""
        buffer = np.zeros(0, dtype=np.float32)
        for chunk in audio_chunks:
            buffer = np.concatenate([buffer, chunk.astype(np.float32)])
            # Re-transcribe the rolling buffer for a partial (cheap at base size, short utterances).
            partial = self.transcribe(buffer, language=language)
            partial["final"] = False
            yield partial
        final = self.transcribe(buffer, language=language)
        final["final"] = True
        yield final
