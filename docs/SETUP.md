# Ultron-OS — Setup

## Prerequisites
- Java 21 (or Java 17 with `-Djava.version=17` for local Maven), Node 20+, Docker, Python 3.12 (for ai-layer/voice ML wheels).
- Optional: Ollama (`ollama pull llama3.1` + `ollama pull nomic-embed-text`).

## 1. Infrastructure
```bash
cp .env.example .env
docker compose up -d postgres            # Postgres 16 + pgvector
docker compose --profile llm up -d ollama   # optional local LLM
```

## 2. Backend (the brain server)
```bash
cd backend
./mvnw spring-boot:run                    # http://localhost:8080
./mvnw test                               # full suite (H2, offline)
# Local Maven on JDK 17? add:  -Djava.version=17
```
Health: `GET /api/health` → shows brain, embedder, llmActive, workers.

## 3. Frontend control panel
```bash
cd frontend && npm install && npm run dev # http://localhost:5173
```

## 4. AI layer (optional, opt-in)
```bash
cd ai-layer && python -m venv .venv && . .venv/Scripts/activate
pip install -r requirements.txt
uvicorn main:app --port 8000
# enable bridge: ULTRON_PYTHON_BRIDGE_ENABLED=true
python -m unittest discover -s tests -t .  # 27 tests
```

## 5. Voice + HUD (Phase 2)
```bash
docker compose --profile voice up -d livekit hud    # LiveKit + Next.js HUD :3000
cd voice && pip install -r requirements.txt
# download Piper voices + an "Ultron" openWakeWord model into voice/models + language_packs
python verification/voice_id.py enroll sample1.wav sample2.wav sample3.wav   # enroll your voice
python agent.py                                      # run on the host for mic access
```

## 6. Full stack + monitoring
```bash
docker compose --profile full up -d
docker compose --profile monitoring up -d prometheus grafana   # :9090 / :3001
```

## Everything offline, zero paid keys by default
The core boots and is useful with no internet and no API keys. Cloud LLMs, ElevenLabs, brokers, Twilio, and connectors are all opt-in.
