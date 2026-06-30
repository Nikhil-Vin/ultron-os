# Ultron-OS — Roadmap

- **Phase 0 — Make it run** ✅ Spring Boot skeleton, heuristic brain, memory, brief, governance, React brief page, CI.
- **Phase 1 — Brain + RAG + Skill Intake + Workers** ✅ OllamaBrain (real LLM), embeddings, RAG, skill intake + `/api/skills`, SkillAwareRetriever, Planner/Archivist/Scholar, nightly agent loop, psychology core, ai-layer (FastAPI), Python bridge.
- **Phase 2 — Voice + HUD + Work Modes + Multilingual** ✅ streaming voice pipeline (wake/VAD/STT/TTS/barge-in), VoiceController SSE, SpokenResponseFormatter, VoiceProfileManager, VoiceIdGate, WorkModeManager, Next.js HUD + LiveKit, language registry (en/hi/mr + extensible).
- **Phase 3 — Hands + Trading** ✅ ToolRegistry, laptop agent (mTLS), browser extension, KnowledgeGraph, trading module (signals/risk/checklist/psych/journal), brokers (paper wired, live stubbed), trading frontend.
- **Phase 4 — Phone + Connectors** ✅ Termux/Tasker/Shortcuts agents, MCP client, Gmail/Calendar/Notion/Slack/Spotify/Home Assistant connectors, meeting intelligence, smart notifications, financial overview, Twilio extension point.
- **Phase 5 — Polish + Local Optimization + Deploy** ✅ gold-core 3D orb HUD, Function Gemma router, LoRA fine-tuning + eval, deploy pipeline (Netlify/Fly/Railway/Vercel), OWASP + Gitleaks CI, Prometheus + Grafana, multi-sample voice enrollment, n8n workflows, options greeks + candlestick CNN.

## Beyond
Neo4j knowledge graph backend · TimescaleDB for OHLCV · richer device automations · more languages (registry entries) · per-domain LoRA adapters trained on your data.
