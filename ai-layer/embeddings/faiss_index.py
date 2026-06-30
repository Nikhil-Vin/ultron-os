"""In-memory vector index (L4).

A pure-Python brute-force cosine index that needs no dependencies; if ``faiss`` is installed it is
used transparently for speed. Either way the public API is identical, so the rest of the ai-layer
never cares which backend is active.
"""
from __future__ import annotations

from typing import Dict, List, Optional, Sequence, Tuple

from embeddings.embedder import cosine


class BruteForceIndex:
    """Exact cosine search over stored vectors (stdlib only)."""

    name = "bruteforce"

    def __init__(self) -> None:
        self._ids: List[str] = []
        self._vectors: List[List[float]] = []
        self._payloads: Dict[str, dict] = {}

    def add(self, item_id: str, vector: Sequence[float], payload: Optional[dict] = None) -> None:
        self._ids.append(item_id)
        self._vectors.append(list(vector))
        self._payloads[item_id] = payload or {}

    def search(self, query: Sequence[float], top_k: int = 5) -> List[Tuple[str, float, dict]]:
        scored = [
            (item_id, cosine(query, vec), self._payloads.get(item_id, {}))
            for item_id, vec in zip(self._ids, self._vectors)
        ]
        scored = [s for s in scored if s[1] > 0.0]
        scored.sort(key=lambda s: s[1], reverse=True)
        return scored[: max(top_k, 1)]

    def __len__(self) -> int:
        return len(self._ids)


class FaissIndex:
    """Optional faiss-backed index (only if the dependency is installed)."""

    name = "faiss"

    def __init__(self, dimension: int) -> None:
        import faiss  # type: ignore
        import numpy as np  # type: ignore

        self._faiss = faiss
        self._np = np
        self._index = faiss.IndexFlatIP(dimension)  # inner product == cosine on normalised vectors
        self._ids: List[str] = []
        self._payloads: Dict[str, dict] = {}
        self.dimension = dimension

    def add(self, item_id: str, vector, payload: Optional[dict] = None) -> None:
        arr = self._np.asarray([vector], dtype="float32")
        self._index.add(arr)
        self._ids.append(item_id)
        self._payloads[item_id] = payload or {}

    def search(self, query, top_k: int = 5):
        arr = self._np.asarray([query], dtype="float32")
        scores, idxs = self._index.search(arr, min(max(top_k, 1), max(len(self._ids), 1)))
        out = []
        for score, idx in zip(scores[0], idxs[0]):
            if 0 <= idx < len(self._ids) and score > 0:
                item_id = self._ids[idx]
                out.append((item_id, float(score), self._payloads.get(item_id, {})))
        return out

    def __len__(self) -> int:
        return len(self._ids)


def get_index(dimension: int):
    """Return a faiss index when available, else the brute-force fallback."""
    try:
        return FaissIndex(dimension)
    except Exception:  # noqa: BLE001
        return BruteForceIndex()
