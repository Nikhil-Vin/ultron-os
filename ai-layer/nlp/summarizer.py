"""Summarization (NLP).

Stdlib extractive summariser (frequency-scored sentences) that always works offline; upgrades to an
abstractive Transformers pipeline when installed.
"""
from __future__ import annotations

import re
from collections import Counter
from typing import List

_SENT_SPLIT_RE = re.compile(r"(?<=[.!?])\s+")
_WORD_RE = re.compile(r"[a-z']+")
_STOPWORDS = {
    "the", "a", "an", "and", "or", "but", "of", "to", "in", "on", "for", "with", "is", "are",
    "was", "were", "be", "been", "it", "this", "that", "as", "at", "by", "from", "we", "you", "i",
}


def summarize(text: str, max_sentences: int = 3) -> str:
    if not text or not text.strip():
        return ""

    try:
        from transformers import pipeline  # type: ignore

        summariser = pipeline("summarization")
        result = summariser(text, max_length=130, min_length=30, do_sample=False)
        return result[0]["summary_text"]
    except Exception:  # noqa: BLE001 - stdlib extractive fallback
        pass

    sentences = [s.strip() for s in _SENT_SPLIT_RE.split(text.strip()) if s.strip()]
    if len(sentences) <= max_sentences:
        return " ".join(sentences)

    freqs: Counter = Counter()
    for word in _WORD_RE.findall(text.lower()):
        if word not in _STOPWORDS and len(word) > 2:
            freqs[word] += 1

    def sentence_score(sentence: str) -> float:
        words = [w for w in _WORD_RE.findall(sentence.lower()) if w not in _STOPWORDS]
        if not words:
            return 0.0
        return sum(freqs.get(w, 0) for w in words) / len(words)

    ranked = sorted(range(len(sentences)), key=lambda i: sentence_score(sentences[i]), reverse=True)
    chosen = sorted(ranked[:max_sentences])
    return " ".join(sentences[i] for i in chosen)
