"""Sentence-chunked TTS streaming + gapless playback (Section 9.9.A).

Consumes SSE `sentence` events from the backend as they arrive, synthesizes each with Piper the
moment it's complete, and queues the audio for back-to-back playback — so audio starts while the
LLM is still generating. Playback runs on a worker thread and can be cancelled instantly for
barge-in.
"""
from __future__ import annotations

import queue
import threading
from typing import Callable, Optional

import numpy as np

from tts.speak import PiperEngine


class SentenceChunkStreamer:
    def __init__(self, engine: PiperEngine, on_sentence: Optional[Callable[[str, int], None]] = None) -> None:
        self._engine = engine
        self._on_sentence = on_sentence
        self._audio_q: "queue.Queue[Optional[np.ndarray]]" = queue.Queue()
        self._cancel = threading.Event()
        self._worker: Optional[threading.Thread] = None
        self._index = 0

    def start(self) -> None:
        self._cancel.clear()
        self._index = 0
        self._worker = threading.Thread(target=self._playback_loop, daemon=True)
        self._worker.start()

    def submit(self, sentence: str) -> None:
        """Synthesize a sentence and enqueue its audio (non-blocking for the SSE reader)."""
        if self._cancel.is_set() or not sentence.strip():
            return
        if self._on_sentence:
            self._on_sentence(sentence, self._index)
        self._index += 1
        pcm = self._engine.synthesize(sentence)
        if pcm is not None:
            self._audio_q.put(pcm)

    def finish(self) -> None:
        self._audio_q.put(None)  # sentinel → drain + stop

    def cancel(self) -> None:
        """Barge-in: stop playback immediately and flush the queue."""
        self._cancel.set()
        try:
            import sounddevice as sd

            sd.stop()
        except Exception:  # noqa: BLE001
            pass
        with self._audio_q.mutex:
            self._audio_q.queue.clear()
        self._audio_q.put(None)

    def _playback_loop(self) -> None:
        try:
            import sounddevice as sd
        except Exception:  # noqa: BLE001
            sd = None
        while not self._cancel.is_set():
            pcm = self._audio_q.get()
            if pcm is None:
                break
            if sd is not None:
                sd.play(pcm, samplerate=self._engine.sample_rate)
                sd.wait()
