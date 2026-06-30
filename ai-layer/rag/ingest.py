"""Document ingestion (L3): normalize → chunk.

Pure-stdlib chunking by paragraphs with a soft character budget and overlap. LlamaIndex/LangChain
loaders can replace this later; the chunk contract (list of dicts) stays the same.
"""
from __future__ import annotations

import re
from typing import List

_WHITESPACE_RE = re.compile(r"[ \t]+")


def normalize(text: str) -> str:
    if not text:
        return ""
    # Collapse runs of spaces/tabs, strip trailing space per line, drop excess blank lines.
    lines = [_WHITESPACE_RE.sub(" ", ln).rstrip() for ln in text.splitlines()]
    cleaned = "\n".join(lines)
    return re.sub(r"\n{3,}", "\n\n", cleaned).strip()


def chunk(text: str, max_chars: int = 800, overlap: int = 100) -> List[str]:
    """Greedy paragraph-aware chunking with character overlap between chunks."""
    text = normalize(text)
    if not text:
        return []
    paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
    chunks: List[str] = []
    buffer = ""
    for para in paragraphs:
        if not buffer:
            buffer = para
        elif len(buffer) + 2 + len(para) <= max_chars:
            buffer = f"{buffer}\n\n{para}"
        else:
            chunks.append(buffer)
            tail = buffer[-overlap:] if overlap > 0 else ""
            buffer = (tail + "\n\n" + para).strip() if tail else para
    if buffer:
        chunks.append(buffer)

    # Hard-split any oversized single paragraph.
    out: List[str] = []
    for c in chunks:
        if len(c) <= max_chars:
            out.append(c)
        else:
            for i in range(0, len(c), max_chars - overlap):
                out.append(c[i : i + max_chars])
    return out


def ingest(text: str, source: str = "manual", max_chars: int = 800, overlap: int = 100) -> List[dict]:
    """Produce chunk records ready for embedding + indexing."""
    pieces = chunk(text, max_chars=max_chars, overlap=overlap)
    return [
        {"chunk_id": f"{source}#{i}", "source": source, "ordinal": i, "text": piece}
        for i, piece in enumerate(pieces)
    ]
