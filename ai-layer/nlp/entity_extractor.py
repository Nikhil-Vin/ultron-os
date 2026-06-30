"""Entity extraction (NLP). Stdlib regex extractor with optional spaCy NER upgrade."""
from __future__ import annotations

import re
from typing import Dict, List

_EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
_URL_RE = re.compile(r"https?://[^\s)]+")
_MONEY_RE = re.compile(r"[$₹€£]\s?\d[\d,]*(?:\.\d+)?")
_DATE_RE = re.compile(r"\b\d{4}-\d{2}-\d{2}\b")
# Sequences of Capitalised words → candidate proper nouns.
_PROPER_RE = re.compile(r"\b([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)\b")


def extract(text: str) -> Dict[str, List[str]]:
    try:
        import spacy  # type: ignore

        nlp = spacy.load("en_core_web_sm")
        doc = nlp(text or "")
        ents: Dict[str, List[str]] = {}
        for ent in doc.ents:
            ents.setdefault(ent.label_, [])
            if ent.text not in ents[ent.label_]:
                ents[ent.label_].append(ent.text)
        ents["_backend"] = ["spacy"]
        return ents
    except Exception:  # noqa: BLE001 - stdlib regex fallback
        pass

    text = text or ""
    return {
        "_backend": ["regex"],
        "emails": _dedupe(_EMAIL_RE.findall(text)),
        "urls": _dedupe(_URL_RE.findall(text)),
        "money": _dedupe(_MONEY_RE.findall(text)),
        "dates": _dedupe(_DATE_RE.findall(text)),
        "proper_nouns": _dedupe(_PROPER_RE.findall(text)),
    }


def _dedupe(items: List[str]) -> List[str]:
    seen: List[str] = []
    for it in items:
        if it not in seen:
            seen.append(it)
    return seen
