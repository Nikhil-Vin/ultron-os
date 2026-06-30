"""EPUB extraction. Optional dependency: ebooklib (+ the stdlib HTML stripper from web_scraper)."""
from __future__ import annotations


class OptionalDependencyError(RuntimeError):
    pass


def available() -> bool:
    try:
        import ebooklib  # type: ignore # noqa: F401

        return True
    except Exception:  # noqa: BLE001
        return False


def parse(path: str) -> str:
    if not available():
        raise OptionalDependencyError("EPUB intake requires 'ebooklib' (pip install EbookLib).")
    from ebooklib import epub, ITEM_DOCUMENT  # type: ignore

    from skills.web_scraper import _strip_html

    book = epub.read_epub(path)
    parts = []
    for item in book.get_items_of_type(ITEM_DOCUMENT):
        parts.append(_strip_html(item.get_content().decode("utf-8", errors="replace")))
    return "\n\n".join(p for p in parts if p)
