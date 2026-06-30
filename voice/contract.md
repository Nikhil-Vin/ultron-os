# Ultron Render-Event Contract (v1)

> **Single source of truth** for the JSON messages exchanged between the Python voice agent
> (`voice/agent.py`) and the Next.js HUD (`web/`) over the **LiveKit data channel**. Read this
> before touching either side. Never improvise a new event shape per feature — extend this file.

## Transport

- Both the agent and the HUD join the **same LiveKit room** (default `ultron`).
- Events are published on the data channel as **UTF-8 JSON**, one object per message, using
  LiveKit's reliable data channel (`rtc.DataPacketKind.KIND_RELIABLE`).
- The HUD subscribes to `room.on("data_received")` and dispatches by the `type` field.

## Envelope

Every message is a JSON object with this envelope:

```json
{
  "type": "string",        // one of the event types below
  "ts": 1730000000000,     // epoch millis (number)
  "topic": "string",       // optional logical grouping
  "payload": { }           // type-specific object (see below)
}
```

## Event types (agent → HUD)

### `state` — voice turn-state machine
Drives `VoiceBar.tsx`. Mirrors `turn_state_machine.py`.
```json
{ "type": "state", "payload": { "state": "listening|thinking|speaking|interrupted|idle" } }
```

### `transcript` — live STT (partial + final)
```json
{ "type": "transcript", "payload": { "text": "deploy the frontend", "final": false, "lang": "en" } }
```

### `sentence` — a spoken sentence chunk (synced to TTS playback)
Drives `BriefPanel.tsx` letter-by-letter type-out, timed to audio.
```json
{ "type": "sentence", "payload": { "text": "Your deploy command is netlify deploy prod.", "index": 0 } }
```

### `brief` — daily/overnight brief panel
```json
{ "type": "brief", "payload": { "title": "Morning Brief", "lines": ["...", "..."] } }
```

### `metrics` — self-drawing chart (`MetricsChart.tsx`)
```json
{ "type": "metrics", "payload": { "title": "P&L (7d)", "unit": "₹",
  "series": [ { "label": "Mon", "value": 1200 }, { "label": "Tue", "value": -300 } ] } }
```

### `pipeline` — funnel / pipeline (`PipelineFunnel.tsx`)
```json
{ "type": "pipeline", "payload": { "title": "Trade signals",
  "stages": [ { "label": "Scanned", "value": 40, "atRisk": false },
              { "label": "Setups", "value": 6, "atRisk": false },
              { "label": "Triggered", "value": 1, "atRisk": true } ] } }
```

### `intel` — research/highlights timeline (`IntelTimeline.tsx`)
```json
{ "type": "intel", "payload": { "title": "SEBI F&O", "items": [ { "when": "09:15", "text": "..." } ] } }
```

### `actions` — task/action list (`ActionsList.tsx`)
```json
{ "type": "actions", "payload": { "title": "Today", "items": [ { "text": "Renew domain", "done": false } ] } }
```

## Control events (HUD → agent)

### `wake` — UI tap to start Live Mode (equivalent to the wake word)
```json
{ "type": "wake", "payload": { "source": "ui" } }
```

### `end` — end the Live Mode session
```json
{ "type": "end", "payload": {} }
```

## Tool → event mapping

`voice/tools.py` returns BOTH a `spoken` string (for TTS) and a `render` event from this list:

| Tool | render `type` |
|---|---|
| `get_daily_brief` | `brief` |
| `show_metrics` | `metrics` |
| `get_pipeline` | `pipeline` |
| `research_intel` | `intel` |
| `plan_my_day` | `actions` |

## Versioning

Bump the `v1` in this filename's heading and add a `"v": 2` field to the envelope when a
breaking change lands. Additive fields don't require a version bump.
