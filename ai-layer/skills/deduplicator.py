"""Chunk deduplication via cosine similarity (stdlib).

Removes near-duplicate chunks before they are stored, so re-ingesting overlapping documents does
not bloat the index. Uses the active embedder; threshold is tunable.
"""
from __future__ import annotations

from typing import List

from embeddings.embedder import cosine, get_embedder


def deduplicate(chunks: List[dict], threshold: float = 0.95, embedder=None) -> List[dict]:
    """Return chunks with near-duplicates (cosine >= threshold) removed, order preserved."""
    if not chunks:
        return []
    embedder = embedder or get_embedder()
    kept: List[dict] = []
    kept_vectors: List[List[float]] = []
    for rec in chunks:
        vec = embedder.embed(rec.get("text", ""))
        if any(cosine(vec, kv) >= threshold for kv in kept_vectors):
            continue
        kept.append(rec)
        kept_vectors.append(vec)
    return kept
