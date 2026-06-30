# Skill Intake — Teaching Ultron New Skills (Phase 1)

> "Feed it any skill." Skill Intake lets you teach Ultron a procedure, playbook, or body of
> knowledge once, and have it recalled as grounding context for every future answer and plan.

## What ships in Phase 1 (backend, offline)

The Java backend implements the **core intake + retrieval loop**, fully offline with zero paid keys:

```
POST /api/skills            → learn a skill (gated LOW → audited → embedded → stored)
GET  /api/skills?q=&limit=  → keyword search / list recent skills
POST /api/ask               → ask a question; answer is grounded in memories + skills (RAG)
```

### Pipeline

```
            ┌─────────────┐   ┌──────────────┐   ┌────────────┐   ┌─────────────┐
intake ───▶ │ ApprovalGate│──▶│  Embedder    │──▶│ SkillRepo  │──▶│  pgvector / │
(name,      │ (LOW, audit)│   │ (heuristic / │   │ (Postgres) │   │  Java cosine│
 content)   └─────────────┘   │  Ollama)     │   └────────────┘   └─────────────┘
                              └──────────────┘
```

1. **Govern** — `SkillService.intake()` proposes a `skill.intake` action (`RiskLevel.LOW`); the
   `ApprovalGate` auto-approves it and writes an immutable `audit_log` entry.
2. **Embed** — the text (`name + description + content + tags`) is embedded by the active
   `Embedder`. Offline default is `HeuristicEmbedder`; a reachable Ollama server is used
   automatically (`OllamaEmbedder`), degrading safely when absent.
3. **Store** — persisted to the `skills` table (`V2__rag_and_skills.sql`) with the embedding as a
   portable CSV vector.
4. **Retrieve** — `RagService` ranks skills + memories by cosine similarity (lexical Jaccard
   fallback when an embedding is missing/incompatible). `SkillAwareRetriever` boosts skill-sourced
   matches so explicitly-taught knowledge outranks incidental notes.
5. **Ground** — the active `Brain` answers using only the retrieved context.

### Example

```bash
# Teach a skill
curl -X POST http://localhost:8080/api/skills \
  -H 'content-type: application/json' \
  -d '{"name":"Deploy frontend","description":"Ship the React app",
       "content":"Run npm run build then netlify deploy --prod","tags":"devops,frontend"}'

# Ask — grounded in what you taught
curl -X POST http://localhost:8080/api/ask \
  -H 'content-type: application/json' \
  -d '{"question":"how do I deploy the frontend?","topK":5}'
```

## Governance & safety

- Intake is **LOW risk**: auto-approved but always audited (no silent writes).
- All intake text passes `InputSanitizer` at the boundary (control-char strip, length cap,
  prompt-injection flagging). Ingested content is treated as **data, never instructions**.
- Webhook ingestion (`POST /api/webhook`) is rate-limited per source via `RateLimiter`.

## What the Python `ai-layer` adds later (Phase 1+ / Phase 2)

The heavy document-processing front-end of intake runs in the optional FastAPI `ai-layer`
(disabled by default; see `PythonBridgeConfig`):

| Source        | Module                         | Library                     |
|---------------|--------------------------------|-----------------------------|
| PDF           | `skills/pdf_parser.py`         | PyMuPDF + pdfplumber        |
| Web page      | `skills/web_scraper.py`        | Playwright + trafilatura    |
| YouTube       | `skills/youtube_ingest.py`     | yt-dlp + faster-whisper     |
| EPUB          | `skills/epub_parser.py`        | ebooklib                    |
| Code          | `skills/code_parser.py`        | tree-sitter                 |
| Dedup         | `skills/deduplicator.py`       | cosine-similarity dedup     |
| Embeddings    | `embeddings/embedder.py`       | Sentence-Transformers       |
| Vector search | `embeddings/faiss_index.py`    | Faiss                       |

Flow: **fetch → normalize → chunk → embed → dedup → store**. The Java `IngestionService` calls the
bridge when enabled; otherwise the offline path above remains fully functional.
