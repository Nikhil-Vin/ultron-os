"""Decision memory (L7).

Stores past decisions with their embedding and recalls the most similar prior decisions for a new
situation, so the system can stay consistent with how it acted before. Stdlib cosine recall.
"""
from __future__ import annotations

from typing import List

from embeddings.embedder import cosine, get_embedder


class DecisionMemory:
    def __init__(self, embedder=None) -> None:
        self._embedder = embedder or get_embedder()
        self._items: List[dict] = []  # {"situation","decision","outcome","vector"}

    def record(self, situation: str, decision: str, outcome: str = "") -> None:
        self._items.append(
            {
                "situation": situation,
                "decision": decision,
                "outcome": outcome,
                "vector": self._embedder.embed(situation),
            }
        )

    def recall(self, situation: str, top_k: int = 3) -> List[dict]:
        if not situation or not self._items:
            return []
        qv = self._embedder.embed(situation)
        scored = [
            {
                "situation": it["situation"],
                "decision": it["decision"],
                "outcome": it["outcome"],
                "score": round(cosine(qv, it["vector"]), 6),
            }
            for it in self._items
        ]
        scored = [s for s in scored if s["score"] > 0.0]
        scored.sort(key=lambda s: s["score"], reverse=True)
        return scored[: max(top_k, 1)]

    def __len__(self) -> int:
        return len(self._items)
