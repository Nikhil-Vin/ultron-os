# Ultron-OS

> A voice-first, local-first personal AI you completely own. Pretrained LLM brain that knows YOUR data (RAG + growing memory), learns any skill you feed it, and acts on your devices — with a human-in-the-loop approval gate guarding every consequential action.

This repository is built phase-by-phase. You are looking at **Phase 0 — Make It Run**.

## Architecture (8 layers, dependencies flow DOWN only)

| Layer | Name | Role |
|---|---|---|
| L0 | Interface | React + Tailwind + Charts (The Face) |
| L1 | Kernel | Spring Boot routing + scheduler + agent loop (Nervous System) |
| L2 | Workers | Sentinel · Planner · Archivist · Scholar · Trader (The Staff) |
| L3 | Intelligence | Spring AI + RAG + Skill Intake + Tools (The Brain) |
| L4 | Memory | PostgreSQL + pgvector + Skills (The Moat) |
| L5 | Connectors | MCP + Device Agents + Brokers + Apps (The Hands) |
| L6 | Governance | Approval Gate + Audit + Voice Biometric (The Conscience) |
| L7 | Psychology | Behavior + Intent + Mood + Feedback (The Mind) |

## Phase 0 scope (what runs today)

- Spring Boot backend (Java 21, Spring Boot 3.3) with the L1–L6 skeleton.
- `HeuristicBrain` — fully offline default reasoning. `OllamaBrain` auto-used only if a local Ollama server is reachable; otherwise falls back to heuristic.
- Memory layer: save + keyword recall (PostgreSQL via Flyway; H2 in tests).
- GitHub fixture connector → readable overnight brief (no network, no keys).
- Governance: `RiskLevel` / `ApprovalGate` / `AuditLog`, with `ULTRON_AUTO_APPROVE=false` enforced.
- REST: `GET /api/health`, `POST /api/brief`, `POST /api/memory`, `GET /api/memory?q=`.
- React + Vite + Tailwind frontend rendering the morning brief.

**Zero paid API keys. Fully offline-capable.**

## Quick start

### 1. Infrastructure (Postgres + pgvector + Ollama)
```bash
cp .env.example .env
docker compose up -d postgres        # Postgres 16 + pgvector
# optional local LLM:
docker compose up -d ollama
```

### 2. Backend
```bash
cd backend
./mvnw spring-boot:run               # http://localhost:8080
./mvnw test                          # run the test suite
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev                          # http://localhost:5173
```

## API smoke test
```bash
curl http://localhost:8080/api/health
curl -X POST http://localhost:8080/api/brief
curl -X POST http://localhost:8080/api/memory \
  -H 'content-type: application/json' \
  -d '{"content":"Reviewed the pgvector index plan","type":"NOTE","tags":["infra"]}'
curl "http://localhost:8080/api/memory?q=pgvector"
```

## The Moat (never violated)
- **Local-first** — your data stays on your hardware by default.
- **Model-agnostic** — swap Ollama → OpenAI → Anthropic in one config line.
- **Free & open-source by default** — zero paid keys to run the core.
- **Human-in-the-loop** — every mutating action passes the approval gate + audit log.

See `docs/` (added in later phases) for full strategy, security, and roadmap.
