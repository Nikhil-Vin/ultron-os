"""RAG retriever (L3): embed → index → semantic recall.

Holds an in-memory index of ingested chunks and answers top-k queries. This is the offline analogue
of the Java RagService; the Spring backend can delegate here when the python-bridge is enabled.
"""
from __future__ import annotations

from typing import List

from embeddings.embedder import get_embedder
from embeddings.faiss_index import get_index
from rag.ingest import ingest as ingest_chunks


class Retriever:
    def __init__(self) -> None:
        self._embedder = get_embedder()
        self._index = get_index(getattr(self._embedder, "dimension", 512))

    @property
    def backend(self) -> dict:
        return {"embedder": self._embedder.name, "index": self._index.name, "chunks": len(self._index)}

    def ingest(self, text: str, source: str = "manual") -> int:
        records = ingest_chunks(text, source=source)
        for rec in records:
            vector = self._embedder.embed(rec["text"])
            self._index.add(rec["chunk_id"], vector, payload=rec)
        return len(records)

    def query(self, query: str, top_k: int = 5) -> List[dict]:
        if not query or not query.strip():
            return []
        qv = self._embedder.embed(query)
        hits = self._index.search(qv, top_k=top_k)
        return [
            {
                "chunk_id": item_id,
                "score": round(score, 6),
                "source": payload.get("source"),
                "text": payload.get("text"),
            }
            for item_id, score, payload in hits
        ]
