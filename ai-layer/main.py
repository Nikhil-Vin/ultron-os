"""Ultron-OS ai-layer — FastAPI app (Python deep-learning services, L3/L7).

Design principle (matches the Java backend): every endpoint works offline with deterministic
stdlib logic and upgrades automatically when optional ML dependencies are installed. The Spring
backend calls this service only when ``ultron.python-bridge.enabled=true``.

Auth: if ULTRON_PYTHON_BRIDGE_KEY is set, requests must send a matching ``X-API-Key`` header.
When unset (local default), the service is open on localhost.
"""
from __future__ import annotations

import os
from typing import List, Optional

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel

from anomaly.detector import detect as detect_anomalies
from embeddings.embedder import get_embedder
from nlp.entity_extractor import extract as extract_entities
from nlp.summarizer import summarize as summarize_text
from psychology.behavior_profiler import profile as profile_behavior
from psychology.feedback_loop import FeedbackLoop
from psychology.intent_classifier import classify as classify_intent
from psychology.mood_detector import detect as detect_mood
from psychology.priority_scorer import score as score_priority
from rag.retriever import Retriever
from skills.intake import intake as run_intake

app = FastAPI(title="Ultron-OS ai-layer", version="0.1.0-PHASE1")

# Shared singletons (in-memory; a solo-operator local service).
_embedder = get_embedder()
_retriever = Retriever()
_feedback = FeedbackLoop()


def require_api_key(x_api_key: Optional[str] = Header(default=None)) -> None:
    expected = os.getenv("ULTRON_PYTHON_BRIDGE_KEY", "")
    if expected and x_api_key != expected:
        raise HTTPException(status_code=401, detail="invalid or missing X-API-Key")


# ----------------------------- request models ------------------------------
class TextIn(BaseModel):
    text: str


class IngestIn(BaseModel):
    text: str
    source: str = "manual"


class QueryIn(BaseModel):
    query: str
    topK: int = 5


class IntakeIn(BaseModel):
    name: str
    text: Optional[str] = None
    url: Optional[str] = None
    content_type: Optional[str] = None
    dedup: bool = True


class SummarizeIn(BaseModel):
    text: str
    maxSentences: int = 3


class SeriesIn(BaseModel):
    series: List[float]


class FeedbackIn(BaseModel):
    intent: str
    positive: bool


# --------------------------------- routes ----------------------------------
@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "service": "ai-layer",
        "embedder": _embedder.name,
        "rag": _retriever.backend,
    }


@app.post("/embed", dependencies=[Depends(require_api_key)])
def embed(body: TextIn) -> dict:
    vec = _embedder.embed(body.text)
    return {"backend": _embedder.name, "dim": len(vec), "vector": vec}


@app.post("/rag/ingest", dependencies=[Depends(require_api_key)])
def rag_ingest(body: IngestIn) -> dict:
    count = _retriever.ingest(body.text, source=body.source)
    return {"ingested_chunks": count, "index": _retriever.backend}


@app.post("/rag/query", dependencies=[Depends(require_api_key)])
def rag_query(body: QueryIn) -> dict:
    return {"results": _retriever.query(body.query, top_k=body.topK)}


@app.post("/skills/intake", dependencies=[Depends(require_api_key)])
def skills_intake(body: IntakeIn) -> dict:
    try:
        return run_intake(
            name=body.name,
            text=body.text,
            url=body.url,
            content_type=body.content_type,
            dedup=body.dedup,
        )
    except Exception as exc:  # noqa: BLE001 - surface optional-dep + validation errors cleanly
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@app.post("/psychology/intent", dependencies=[Depends(require_api_key)])
def psychology_intent(body: TextIn) -> dict:
    return {"intent": classify_intent(body.text)}


@app.post("/psychology/priority", dependencies=[Depends(require_api_key)])
def psychology_priority(body: TextIn) -> dict:
    return {"score": score_priority(body.text)}


@app.post("/psychology/mood", dependencies=[Depends(require_api_key)])
def psychology_mood(body: TextIn) -> dict:
    return detect_mood(body.text)


@app.post("/psychology/feedback", dependencies=[Depends(require_api_key)])
def psychology_feedback(body: FeedbackIn) -> dict:
    _feedback.record(body.intent, body.positive)
    return {"intent": body.intent, "weight": _feedback.weight(body.intent)}


@app.post("/psychology/behavior", dependencies=[Depends(require_api_key)])
def psychology_behavior(events: List[dict]) -> dict:
    return profile_behavior(events)


@app.post("/nlp/entities", dependencies=[Depends(require_api_key)])
def nlp_entities(body: TextIn) -> dict:
    return {"entities": extract_entities(body.text)}


@app.post("/nlp/summarize", dependencies=[Depends(require_api_key)])
def nlp_summarize(body: SummarizeIn) -> dict:
    return {"summary": summarize_text(body.text, max_sentences=body.maxSentences)}


@app.post("/anomaly/detect", dependencies=[Depends(require_api_key)])
def anomaly_detect(body: SeriesIn) -> dict:
    return detect_anomalies(body.series)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=int(os.getenv("AI_LAYER_PORT", "8000")))
