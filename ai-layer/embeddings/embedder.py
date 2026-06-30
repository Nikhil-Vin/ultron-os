"""Embedding service (L3).

Mirrors the Java side: a deterministic, dependency-free ``HashingEmbedder`` is always available,
and a Sentence-Transformers model is used automatically when the optional dependency is installed.
This keeps the ai-layer fully runnable offline on any Python, while allowing a real-model upgrade.
"""
from __future__ import annotations

import hashlib
import math
import re
from typing import List, Sequence

# Match the Java HeuristicEmbedder dimension so vectors are interchangeable across the bridge.
DIMENSION = 512

_TOKEN_RE = re.compile(r"[^a-z0-9]+")


def tokenize(text: str) -> List[str]:
    if not text:
        return []
    return [t for t in _TOKEN_RE.split(text.lower()) if len(t) >= 2]


def l2normalize(vec: List[float]) -> List[float]:
    norm = math.sqrt(sum(x * x for x in vec))
    if norm == 0.0:
        return vec
    return [x / norm for x in vec]


def cosine(a: Sequence[float], b: Sequence[float]) -> float:
    if not a or not b or len(a) != len(b):
        return 0.0
    dot = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(y * y for y in b))
    if na == 0.0 or nb == 0.0:
        return 0.0
    return dot / (na * nb)


class HashingEmbedder:
    """Deterministic hashed bag-of-words embedder (stdlib only)."""

    name = "hashing"

    def __init__(self, dimension: int = DIMENSION) -> None:
        self.dimension = dimension

    def embed(self, text: str) -> List[float]:
        vec = [0.0] * self.dimension
        for token in tokenize(text):
            # Stable hash (md5) so results are identical across processes/runs.
            digest = hashlib.md5(token.encode("utf-8")).hexdigest()
            bucket = int(digest, 16) % self.dimension
            vec[bucket] += 1.0
        return l2normalize(vec)


class SentenceTransformerEmbedder:
    """Optional upgrade backed by sentence-transformers (only if installed)."""

    name = "sentence-transformers"

    def __init__(self, model_name: str = "all-MiniLM-L6-v2") -> None:
        from sentence_transformers import SentenceTransformer  # type: ignore

        self._model = SentenceTransformer(model_name)
        self.dimension = int(self._model.get_sentence_embedding_dimension())

    def embed(self, text: str) -> List[float]:
        vec = self._model.encode(text or "", normalize_embeddings=True)
        return [float(x) for x in vec]


def get_embedder():
    """Return the best available embedder, degrading safely to the hashing fallback."""
    try:
        return SentenceTransformerEmbedder()
    except Exception:  # noqa: BLE001 - any import/load failure → offline fallback
        return HashingEmbedder()
