"""Skill Intake orchestrator (L3).

Detects the source format, routes to the right extractor (txt/md handled in-stdlib; pdf/web/youtube/
epub via optional dependencies), then normalises → chunks → dedups. Mirrors SKILL_INTAKE.md.
"""
from __future__ import annotations

import os
from typing import Optional

from rag.ingest import ingest as ingest_chunks
from skills.deduplicator import deduplicate


def detect_format(*, filename: Optional[str] = None, url: Optional[str] = None,
                  content_type: Optional[str] = None) -> str:
    if url:
        u = url.lower()
        if "youtube.com" in u or "youtu.be" in u:
            return "youtube"
        return "web"
    if content_type:
        ct = content_type.lower()
        if "pdf" in ct:
            return "pdf"
        if "epub" in ct:
            return "epub"
        if "html" in ct:
            return "web"
    if filename:
        ext = os.path.splitext(filename)[1].lower()
        return {
            ".pdf": "pdf",
            ".epub": "epub",
            ".md": "markdown",
            ".markdown": "markdown",
            ".txt": "text",
            ".html": "web",
            ".htm": "web",
        }.get(ext, "text")
    return "text"


def extract(fmt: str, *, text: Optional[str] = None, path: Optional[str] = None,
            url: Optional[str] = None) -> str:
    """Return raw text for a detected format. Optional backends raise if their deps are missing."""
    if fmt in ("text", "markdown"):
        if text is not None:
            return text
        if path:
            with open(path, "r", encoding="utf-8", errors="replace") as fh:
                return fh.read()
        raise ValueError("text/markdown intake requires 'text' or 'path'")
    if fmt == "pdf":
        from skills import pdf_parser

        return pdf_parser.parse(path or "")
    if fmt == "epub":
        from skills import epub_parser

        return epub_parser.parse(path or "")
    if fmt == "web":
        from skills import web_scraper

        return web_scraper.fetch(url or "")
    if fmt == "youtube":
        from skills import youtube_ingest

        return youtube_ingest.transcribe(url or "")
    raise ValueError(f"unsupported format: {fmt}")


def intake(*, name: str, text: Optional[str] = None, path: Optional[str] = None,
           url: Optional[str] = None, content_type: Optional[str] = None,
           dedup: bool = True) -> dict:
    """Full intake pipeline → returns the detected format and the (deduped) chunk records."""
    filename = path or name
    fmt = detect_format(filename=filename, url=url, content_type=content_type)
    raw = extract(fmt, text=text, path=path, url=url)
    source = name or url or path or "skill"
    chunks = ingest_chunks(raw, source=source)
    if dedup:
        chunks = deduplicate(chunks)
    return {"name": name, "format": fmt, "source": source, "chunk_count": len(chunks), "chunks": chunks}
