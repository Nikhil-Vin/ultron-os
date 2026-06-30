"""Turn-taking state machine for Live Mode (Section 9.9.E).

States: idle → listening → thinking → speaking → (interrupted) → listening …
Publishes a `state` render event on every transition so VoiceBar.tsx animates in lockstep.
"""
from __future__ import annotations

import time
from enum import Enum
from typing import Callable, Optional


class TurnState(str, Enum):
    IDLE = "idle"
    LISTENING = "listening"
    THINKING = "thinking"
    SPEAKING = "speaking"
    INTERRUPTED = "interrupted"


class TurnStateMachine:
    def __init__(self, on_change: Optional[Callable[[TurnState], None]] = None) -> None:
        self._state = TurnState.IDLE
        self._on_change = on_change

    @property
    def state(self) -> TurnState:
        return self._state

    def to(self, state: TurnState) -> None:
        if state == self._state:
            return
        self._state = state
        if self._on_change:
            self._on_change(state)

    # Convenience transitions
    def listening(self) -> None:
        self.to(TurnState.LISTENING)

    def thinking(self) -> None:
        self.to(TurnState.THINKING)

    def speaking(self) -> None:
        self.to(TurnState.SPEAKING)

    def interrupted(self) -> None:
        self.to(TurnState.INTERRUPTED)

    def idle(self) -> None:
        self.to(TurnState.IDLE)

    @staticmethod
    def now_ms() -> int:
        return int(time.time() * 1000)
