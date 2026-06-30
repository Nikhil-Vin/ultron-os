# Ultron-OS — Complete Startup Guide

Follow this top to bottom from a fresh `git clone`. Commands are Windows/PowerShell-first (notes for
macOS/Linux inline). **The core runs fully offline with zero paid keys** — cloud LLMs, voice, and
connectors are all opt-in.

```powershell
git clone https://github.com/Nikhil-Vin/ultron-os.git
cd ultron-os
```

Prereqs: Docker Desktop, Java 21 (or 17 — see notes), Node 20+, Python 3.12 (for ai-layer/voice ML),
and optionally Ollama.

---

## 1. Start Docker services

```powershell
copy .env.example .env          # macOS/Linux: cp .env.example .env

# Minimum: just the database
docker compose up -d postgres

# Optional local LLM
docker compose --profile llm up -d ollama

# Optional: AI layer, LiveKit + HUD, monitoring
docker compose --profile ai up -d ai-layer
docker compose --profile voice up -d livekit hud
docker compose --profile monitoring up -d prometheus grafana

# OR everything at once (backend in Docker too):
docker compose --profile full --profile voice --profile monitoring up -d
```
Check: `docker ps` → `ultron-postgres` should be `(healthy)`.

Ports: Postgres 5432 · Ollama 11434 · backend 8080 · ai-layer 8000 · LiveKit 7880 · HUD 3000 ·
Prometheus 9090 · Grafana 3001.

---

## 2. Pull Ollama models (free, local — makes the brain a real LLM)

```powershell
ollama pull llama3.1            # reasoning (chat)
ollama pull nomic-embed-text    # embeddings (RAG)
```
If you skip this, Ultron runs on the offline `HeuristicBrain` + hashing embedder (still works).

---

## 3. Python deps — ai-layer + voice agent

```powershell
# AI layer (FastAPI: embeddings, RAG, skill parsing, psychology, trading, routing)
cd ai-layer
python -m venv .venv
.\.venv\Scripts\activate          # macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
python -m unittest discover -s tests -t .   # expect: Ran 27 tests ... OK
deactivate
cd ..

# Voice agent (wake/STT/TTS/VAD/speaker-id) — install on the HOST for mic access
cd voice
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
deactivate
cd ..
```
Heavy ML wheels (torch, faster-whisper, etc.) need Python ≤ 3.12. Every module degrades to a
stdlib fallback if its optional dep is missing.

---

## 4. Node deps — frontend + laptop agent

```powershell
cd frontend && npm install && cd ..
cd agents\laptop && npm install && cd ..\..
# Optional HUD (Next.js):
cd web && npm install && cd ..
```

---

## 5. Start each service

```powershell
# Backend (the brain server). Local Maven on JDK 17 → keep the -Djava.version=17 flag.
cd backend
.\mvnw.cmd "-Djava.version=17" spring-boot:run      # http://localhost:8080
# (Java 21 installed? drop the flag.)

# Frontend control panel (JARVIS UI) — new terminal
cd frontend
npm run dev                                          # http://localhost:5173

# AI layer — new terminal (from ai-layer, venv active)
uvicorn main:app --port 8000                         # http://localhost:8000/health

# Voice agent — new terminal (from voice, venv active, mic on host)
python agent.py                                      # joins LiveKit, drives /api/voice/ask
```

---

## 6. Minimum `.env` (free-tier only)

The core needs **nothing**. For real cloud reasoning, set **one** free key (pick any):
```
# Easiest free options (set ONE):
GOOGLE_API_KEY=AIza...        # aistudio.google.com → Get API key
GROQ_API_KEY=gsk_...          # console.groq.com/keys  (fastest)
# Local LLM instead of cloud:
ULTRON_BRAIN_OLLAMA_MODEL=llama3.1
ULTRON_BRAIN_OLLAMA_EMBEDDING_MODEL=nomic-embed-text
# Governance default — keep false:
ULTRON_AUTO_APPROVE=false
# Optional security gate (leave blank for local):
ULTRON_API_KEY=
```
Provider priority: OpenAI → Anthropic → Gemini → Groq → GitHub → OpenRouter → NVIDIA → Ollama →
Heuristic. First available wins; verify at `/api/brain/providers`.

---

## 7. Connect your Android phone (Termux)

1. Install **Termux** + **Termux:API** (from F-Droid).
2. Find your laptop's LAN IP: `ipconfig` (e.g. `192.168.1.20`). Allow port 8080 through the firewall.
3. On the phone, in Termux:
```bash
# copy agents/phone/android/ to the phone, or git clone the repo, then:
cd ultron-os/agents/phone/android
ULTRON_WS=ws://192.168.1.20:8080/ws/device/android bash setup.sh
```
That installs deps, sets `ULTRON_WS`, and starts `ultron_phone_agent.py` in the background. The
JARVIS header pill flips to **📱 PHONE: ONLINE**. Try: "open YouTube", "battery kitni hai",
"screenshot le". Calls/messages are CRITICAL → require your verified voice.

---

## 8. Connect your Windows laptop agent

```powershell
cd agents\laptop
npm install
$env:ULTRON_WS = "ws://localhost:8080/ws/device/laptop"
npx tsx agent.ts
```
Header pill flips to **💻 LAPTOP: ONLINE**. Try: "open VS Code", "lock my laptop", "what's my CPU
and RAM". Forbidden actions (delete system files, format) are always refused; non-READ actions need
the backend approval token.

---

## 9. Verify everything works

URLs:
- Backend health → http://localhost:8080/api/health  →  `{"status":"ok","brain":"...","llmActive":...,"workers":[...]}`
- Brain providers → http://localhost:8080/api/brain/providers
- Devices → http://localhost:8080/api/devices
- System vitals → http://localhost:8080/api/system
- Frontend (JARVIS) → http://localhost:5173
- HUD → http://localhost:3000 · Prometheus → http://localhost:9090 · Grafana → http://localhost:3001 (admin/ultron)

API smoke tests (PowerShell):
```powershell
Invoke-RestMethod http://localhost:8080/api/health
# Teach a skill, then ask (grounded answer):
$s = @{ name="Deploy"; content="Run npm run build then netlify deploy --prod"; tags="devops" } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/skills -Method Post -Body $s -ContentType application/json
Invoke-RestMethod http://localhost:8080/api/ask -Method Post -ContentType application/json -Body (@{question="how do I deploy?"} | ConvertTo-Json)
# Agent loop — CRITICAL must be DENIED:
Invoke-RestMethod http://localhost:8080/api/agent -Method Post -ContentType application/json -Body (@{instruction="live trade buy 50 AAPL"} | ConvertTo-Json)
# Trading signal (paper):
Invoke-RestMethod http://localhost:8080/api/trading/signal -Method Post -ContentType application/json -Body (@{instrument="NIFTY50";indicators=@{rsi=28;macd=1.2;macdSignal=0.8}} | ConvertTo-Json)
```
In the JARVIS UI: boot lines type out, vitals show **real** CPU/heap, the BRAIN badge shows the
active provider, device pills show online status, and the mic orb talks back. Hindi/Marathi: just
speak/type in that language — it auto-detects and replies in-language.

---

## 10. Common errors & fixes

| Symptom | Fix |
|---|---|
| `Web server failed to start. Port 8080 was already in use` | `Get-NetTCPConnection -LocalPort 8080 -State Listen \| %{ Stop-Process -Id $_.OwningProcess -Force }` — or run on another port: `--server.port=8081`. |
| `release version 21 not supported` (Maven) | Your Maven uses JDK 17. Add `"-Djava.version=17"` to the mvnw command (already in the run command above). |
| `/api/skills` or `/api/ask` returns 500 about embeddings | Your Ollama was started without `--embeddings`. Either `ollama pull nomic-embed-text`, or ignore — it auto-degrades to the heuristic embedder. |
| `brain: heuristic`, `llmActive: false` | No cloud key set and Ollama not reachable. Set `GROQ_API_KEY`/`GOOGLE_API_KEY` or run `ollama serve` + pull a model. |
| Frontend can't reach API / CORS error | Backend not running, or origin not allowed. Frontend dev server proxies `/api`; ensure backend is on 8080. CORS allows `:5173` and `:3000`. |
| Phone pill stays OFFLINE | Phone + laptop must share Wi-Fi; use the laptop LAN IP (not `localhost`) in `ULTRON_WS`; open port 8080 in the firewall. |
| Voice mic does nothing in the browser | Browser STT needs Chrome/Edge + mic permission (allow on the site). Otherwise type in the input box. |
| `websocket-client` / module not found (phone) | `pip install websocket-client` in Termux. |
| Marathi voice sounds wrong / English voice | Few systems ship a Marathi TTS voice; install one, or use the VOICE picker. Hindi/English are widely available. |
| `git push` says CRLF warnings | Harmless line-ending notices on Windows; ignore. |
| Docker `ai-layer` unhealthy | Heavy ML wheels may not be installed in the image; the service still serves with stdlib fallbacks. Run it on the host if needed. |

Everything is human-in-the-loop: `ULTRON_AUTO_APPROVE=false` is the default, CRITICAL actions need
your verified voice, and live trading / SMS stay disabled extension points until you wire them.
