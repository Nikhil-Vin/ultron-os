"use client";

import { Room, RoomEvent, RemoteParticipant } from "livekit-client";
import { RenderEvent } from "./events";

const LIVEKIT_URL = process.env.NEXT_PUBLIC_LIVEKIT_URL || "ws://localhost:7880";
const ROOM = process.env.NEXT_PUBLIC_ULTRON_ROOM || "ultron";

/** Connect to the shared Ultron room and stream decoded render events to `onEvent`. */
export async function connectHud(onEvent: (e: RenderEvent) => void): Promise<Room> {
  const res = await fetch(`/api/token?room=${encodeURIComponent(ROOM)}`);
  if (!res.ok) throw new Error(`token endpoint failed: ${res.status}`);
  const { token } = await res.json();

  const room = new Room({ adaptiveStream: true });
  const decoder = new TextDecoder();

  room.on(RoomEvent.DataReceived, (payload: Uint8Array, _participant?: RemoteParticipant) => {
    try {
      const event = JSON.parse(decoder.decode(payload)) as RenderEvent;
      onEvent(event);
    } catch {
      /* ignore malformed frames */
    }
  });

  await room.connect(LIVEKIT_URL, token);
  return room;
}

/** Publish a control event back to the agent (wake / end). */
export async function publishControl(room: Room, type: "wake" | "end") {
  const data = new TextEncoder().encode(JSON.stringify({ type, ts: Date.now(), payload: {} }));
  await room.localParticipant.publishData(data, { reliable: true, topic: type });
}

export { ROOM };
