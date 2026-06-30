"""Web page extraction. Optional dependency: trafilatura (preferred) or Playwright for JS pages.

Falls back to a minimal stdlib HTML tag-stripper so plain pages still ingest with zero deps.
"""
from __future__ import annotations

import html
import re
import urllib.request

_TAG_RE = re.compile(r"<[^>]+>")
_SCRIPT_STYLE_RE = re.compile(r"<(script|style)[^>]*>.*?</\1>", re.IGNORECASE | re.DOTALL)


def _strip_html(raw: str) -> str:
    raw = _SCRIPT_STYLE_RE.sub(" ", raw)
    text = _TAG_RE.sub(" ", raw)
    text = html.unescape(text)
    return re.sub(r"\s+", " ", text).strip()


def fetch(url: str, timeout: float = 15.0) -> str:
    """Best-effort article extraction. Prefers trafilatura; degrades to a stdlib tag-stripper."""
    try:
        import trafilatura  # type: ignore

        downloaded = trafilatura.fetch_url(url)
        if downloaded:
            extracted = trafilatura.extract(downloaded)
            if extracted:
                return extracted
    except Exception:  # noqa: BLE001 - fall through to stdlib path
        pass

    req = urllib.request.Request(url, headers={"User-Agent": "ultron-os/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:  # noqa: S310 - explicit user intent
        charset = resp.headers.get_content_charset() or "utf-8"
        raw = resp.read().decode(charset, errors="replace")
    return _strip_html(raw)
