# Ultron-OS — Tech Stack

## Backend (Java — the brain server)
Java 21 · Spring Boot 3.3 (Web, Data JPA, Security, Scheduling, Actuator) · Flyway · PostgreSQL + pgvector · Micrometer/Prometheus · JUnit 5 + Mockito + H2 (offline tests). Model-agnostic `Brain` abstraction (Ollama via REST + streaming; cloud LLMs opt-in).

## AI layer (Python — ai-layer/)
FastAPI + uvicorn + Pydantic v2. Optional ML (all with stdlib fallbacks): Sentence-Transformers, Faiss, LangChain/LlamaIndex, scikit-learn/XGBoost, spaCy, TextBlob, TA-Lib, FinBERT, VectorBT, PyTorch, PEFT/LoRA, transformers, Function Gemma router.

## Voice (voice/)
LiveKit + livekit-agents · openWakeWord · faster-whisper (streaming, multilingual) · Piper TTS · Silero VAD · Resemblyzer (speaker ID) · sounddevice. ElevenLabs/Azure/Deepgram/Twilio opt-in.

## Frontend
Control panel: React 18 + Vite + TypeScript + Tailwind (gold-core theme) + CDN three.js orb. HUD (`web/`): Next.js 14 + @livekit/components-react + framer-motion + recharts.

## Infra
Docker Compose (postgres, ollama, backend, livekit, ai-layer, voice-agent, hud, prometheus, grafana). GitHub Actions CI (build/test, Gitleaks, OWASP dependency-check). Deploy: Netlify (frontend) · Vercel (HUD) · Fly.io (backend) · Railway (ai-layer) · Neon/Supabase (Postgres).

## Devices
Laptop agent (Node + TS, Playwright, mTLS). Browser extension (MV3). Phone (Termux ADB, Tasker, Apple Shortcuts).
