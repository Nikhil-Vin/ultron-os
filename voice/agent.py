"""Ultron LiveKit agent (Section 9.9.F).

Joins the shared LiveKit room as the agent participant, runs the streaming VoicePipeline, and
publishes every pipeline event (state / transcript / sentence) plus tool render events
(brief / metrics / pipeline / intel / actions) on the reliable data channel per voice/contract.md.
The Next.js HUD joins the same room and animates from these events.

Run:  python agent.py
Env:  LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET, ULTRON_ROOM (default 'ultron')
"""
from __future__ import annotations

import asyncio
import json
import os
import threading

from pipeline import VoicePipeline
from tools import TOOLS

LIVEKIT_URL = os.getenv("LIVEKIT_URL", "ws://localhost:7880")
API_KEY = os.getenv("LIVEKIT_API_KEY", "devkey")
API_SECRET = os.getenv("LIVEKIT_API_SECRET", "secret")
ROOM_NAME = os.getenv("ULTRON_ROOM", "ultron")


def _make_token() -> str:
    from livekit import api

    token = (
        api.AccessToken(API_KEY, API_SECRET)
        .with_identity("ultron-agent")
        .with_name("Ultron")
        .with_grants(api.VideoGrants(room_join=True, room=ROOM_NAME, can_publish=True,
                                     can_publish_data=True, can_subscribe=True))
    )
    return token.to_jwt()


class UltronAgent:
    def __init__(self) -> None:
        self._room = None
        self._loop: asyncio.AbstractEventLoop | None = None
        self.pipeline = VoicePipeline(on_event=self._publish_threadsafe)

    async def connect(self) -> None:
        from livekit import rtc

        self._room = rtc.Room()

        @self._room.on("data_received")
        def _on_data(data: rtc.DataPacket):  # HUD → agent control (wake/end)
            try:
                msg = json.loads(data.data.decode("utf-8"))
                if msg.get("type") == "end":
                    self.pipeline.stop()
            except Exception:  # noqa: BLE001
                pass

        await self._room.connect(LIVEKIT_URL, _make_token())
        print(f"[agent] connected to LiveKit room '{ROOM_NAME}' at {LIVEKIT_URL}")

    def _publish_threadsafe(self, event: dict) -> None:
        """Called from the pipeline's worker thread → schedule the async publish on the loop."""
        if self._loop is None or self._room is None:
            return
        asyncio.run_coroutine_threadsafe(self._publish(event), self._loop)

    async def _publish(self, event: dict) -> None:
        from livekit import rtc

        try:
            payload = json.dumps(event).encode("utf-8")
            await self._room.local_participant.publish_data(
                payload, reliable=True, topic=event.get("type", "event"))
        except Exception as exc:  # noqa: BLE001
            print(f"[agent] publish failed: {exc}")

    async def run(self) -> None:
        self._loop = asyncio.get_running_loop()
        await self.connect()
        # Run the (blocking) audio pipeline on a worker thread; events publish back via the loop.
        thread = threading.Thread(target=self.pipeline.run, daemon=True)
        thread.start()
        # Keep the agent alive.
        while thread.is_alive():
            await asyncio.sleep(0.5)


async def _main() -> None:
    agent = UltronAgent()
    await agent.run()


if __name__ == "__main__":
    try:
        asyncio.run(_main())
    except KeyboardInterrupt:
        print("\n[agent] shutting down.")
