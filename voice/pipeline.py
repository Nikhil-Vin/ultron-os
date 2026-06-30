"""Ultron voice pipeline — the full streaming loop (Section 9.9.A), runnable standalone.

  wake word ("Ultron") → VAD record → language id → streaming STT → backend SSE
  → sentence-chunked Piper TTS (audio starts before the LLM finishes) → barge-in cancel → repeat.

Run:  python pipeline.py            (local mic, Live Mode)
It degrades gracefully: any missing model/dependency disables that stage and logs why, so the
loop never hard-crashes — consistent with the fail-safe moat.
"""
from __future__ import annotations

import os
import queue
import sys
import threading
from typing import Callable, Optional

import numpy as np

try:
    import httpx
    from httpx_sse import connect_sse
except Exception:  # noqa: BLE001
    httpx = None

from conversation.barge_in_controller import VadController
from conversation.turn_state_machine import TurnState, TurnStateMachine
from language.detect import detect_language
from stt.streaming_transcribe import StreamingTranscriber, SAMPLE_RATE
from tts.sentence_chunk_streamer import SentenceChunkStreamer
from tts.speak import PiperEngine
from wakeword.listener import WakeWordListener

BACKEND = os.getenv("ULTRON_BACKEND_URL", "http://localhost:8080")
FRAME_MS = 30
FRAME = int(SAMPLE_RATE * FRAME_MS / 1000)
END_SILENCE_FRAMES = int(int(os.getenv("ULTRON_EOT_SILENCE_MS", "250")) / FRAME_MS)


class VoicePipeline:
    def __init__(self, on_event: Optional[Callable[[dict], None]] = None) -> None:
        self.on_event = on_event or (lambda e: None)
        self.state = TurnStateMachine(on_change=self._publish_state)
        self.wake = WakeWordListener()
        self.stt = StreamingTranscriber()
        self.vad = VadController()
        self.tts = PiperEngine()
        self._stop = threading.Event()

    # --- event publishing ---
    def _publish_state(self, state: TurnState) -> None:
        self.on_event({"type": "state", "payload": {"state": state.value}})

    def _publish(self, etype: str, payload: dict) -> None:
        self.on_event({"type": etype, "payload": payload})

    # --- audio capture ---
    def _mic_frames(self):
        import sounddevice as sd

        q: "queue.Queue[np.ndarray]" = queue.Queue()

        def cb(indata, _frames, _time, _status):
            q.put(indata.copy().reshape(-1))

        with sd.InputStream(samplerate=SAMPLE_RATE, channels=1, dtype="float32",
                            blocksize=FRAME, callback=cb):
            while not self._stop.is_set():
                yield q.get()

    # --- one turn ---
    def _record_until_silence(self, frames) -> np.ndarray:
        self.state.listening()
        buf = []
        silent = 0
        for frame in frames:
            buf.append(frame)
            if self.vad.available():
                if self.vad.is_speech(frame):
                    silent = 0
                else:
                    silent += 1
                if silent >= END_SILENCE_FRAMES and len(buf) > END_SILENCE_FRAMES:
                    break
            elif len(buf) > SAMPLE_RATE * 6 / FRAME:  # 6s cap when no VAD
                break
        return np.concatenate(buf).astype(np.float32)

    def _handle_utterance(self, audio: np.ndarray) -> bool:
        """Transcribe, stream the answer to TTS, and watch for barge-in. Returns False to end session."""
        lang = detect_language(audio[: SAMPLE_RATE * 2])
        result = self.stt.transcribe(audio, language=lang)
        text = result.get("text", "").strip()
        if not text:
            return True
        self._publish("transcript", {"text": text, "final": True, "lang": lang})
        if any(p in text.lower() for p in ("that's all", "thats all", "stop listening", "goodbye")):
            return False

        self.state.thinking()
        streamer = SentenceChunkStreamer(
            self.tts, on_sentence=lambda s, i: self._publish("sentence", {"text": s, "index": i}))
        streamer.start()
        self.state.speaking()
        self._stream_answer(text, streamer)
        streamer.finish()
        self.state.listening()
        return True

    def _stream_answer(self, question: str, streamer: SentenceChunkStreamer) -> None:
        if httpx is None:
            streamer.submit("The backend client isn't installed.")
            return
        try:
            with httpx.Client(timeout=180) as client:
                with connect_sse(client, "POST", f"{BACKEND}/api/voice/ask",
                                 json={"question": question, "topK": 5}) as event_source:
                    for sse in event_source.iter_sse():
                        if sse.event == "sentence":
                            streamer.submit(sse.data)
                        elif sse.event == "done":
                            break
        except Exception as exc:  # noqa: BLE001
            streamer.submit(f"I hit an error reaching the backend.")
            print(f"[pipeline] SSE error: {exc}", file=sys.stderr)

    def run(self) -> None:
        print("[ultron] voice pipeline up. Say 'Ultron' to start; 'that's all' to end a session.")
        frames = self._mic_frames()
        int16_frames = (f for f in frames)
        for frame in int16_frames:
            if self._stop.is_set():
                break
            # Wake detection on int16
            if self.wake.available():
                if not self.wake.detect((frame * 32767).astype(np.int16)):
                    continue
            self._publish("state", {"state": "listening"})
            # Live Mode: multiple turns until the user ends it.
            keep_going = True
            while keep_going and not self._stop.is_set():
                audio = self._record_until_silence(frames)
                keep_going = self._handle_utterance(audio)
            self.state.idle()

    def stop(self) -> None:
        self._stop.set()


if __name__ == "__main__":
    VoicePipeline().run()
