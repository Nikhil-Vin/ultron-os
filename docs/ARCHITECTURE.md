# Ultron-OS — Architecture

Dependencies flow **down only**. Nothing references upward.

| Layer | Name | What lives here |
|---|---|---|
| L0 | Interface | React/Vite control panel (gold-core HUD, Core orb, trading, skills…) + Next.js `web/` HUD |
| L1 | Kernel | `Kernel` (dispatch), `Scheduler` (cron), `AgentLoop` (perceive→reason→plan→approve→act→remember), `WorkModeManager` |
| L2 | Workers | Sentinel · Planner · Archivist · Scholar · Trader |
| L3 | Intelligence | `Brain`/`OllamaBrain` (+streaming), `Embedder`, `RagService`, Skill Intake, Trading brain, Psychology, Voice formatter, Language registry, ToolRegistry |
| L4 | Memory | PostgreSQL + embeddings (`memories`, `skills`), `KnowledgeGraph`, audit log |
| L5 | Connectors | GitHub, MCP, Gmail, Calendar, Notion, Slack, Spotify, Home Assistant, Brokers (paper + stubs), Twilio (extension point), device agents |
| L6 | Governance | `RiskLevel`, `ApprovalGate`, `AuditLog`, `VoiceIdGate`, `InputSanitizer`, `RateLimiter` |
| L7 | Psychology | `IntentClassifier`, `PriorityScorer`, `FeedbackLoop`, `TradingPsychMonitor` |

## Process topology
- **backend/** — Spring Boot brain server (Java 21). The single source of truth; everything routes through it.
- **ai-layer/** — FastAPI Python deep-learning services (embeddings, RAG, skill parsing, psychology, trading, routing, fine-tuning). Opt-in via the `PythonBridgeClient`; the JVM degrades to heuristics when it's down.
- **voice/** — LiveKit Python voice agent (wake→VAD→STT→brain→TTS) publishing render events.
- **web/** — Next.js voice-synced HUD joining the same LiveKit room.
- **frontend/** — Vite SPA control panel.
- **agents/** — laptop (mTLS daemon), browser extension, phone (Termux/Tasker/Shortcuts).

## Fail-safe degradation (never crashes)
Ollama down → `HeuristicBrain`. ai-layer down → JVM heuristics. pgvector/embeddings unavailable → keyword search. Embedding model can't embed → hashing embedder. There is always a working path.

## Migrations
`V1` memories+audit+vector ext · `V2` skill embeddings · `V3` skill status · `V4` languages · `V5` trading · `V6` knowledge graph.
