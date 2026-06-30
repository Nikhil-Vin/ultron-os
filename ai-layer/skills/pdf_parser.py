"""PDF extraction. Optional dependency: PyMuPDF (fitz) or pdfplumber.

Returns extracted text when a backend is installed; otherwise raises a clear, actionable error so
the caller can surface "install the optional dependency" rather than crashing the service.
"""
from __future__ import annotations


class OptionalDependencyError(RuntimeError):
    pass


def available() -> bool:
    try:
        import fitz  # type: ignore # noqa: F401

        return True
    except Exception:  # noqa: BLE001
        try:
            import pdfplumber  # type: ignore # noqa: F401

            return True
        except Exception:  # noqa: BLE001
            return False


def parse(path: str) -> str:
    try:
        import fitz  # type: ignore

        doc = fitz.open(path)
        return "\n".join(page.get_text() for page in doc)
    except ImportError:
        pass

    try:
        import pdfplumber  # type: ignore

        with pdfplumber.open(path) as pdf:
            return "\n".join((page.extract_text() or "") for page in pdf.pages)
    except ImportError as exc:
        raise OptionalDependencyError(
            "PDF intake requires 'PyMuPDF' or 'pdfplumber' (pip install PyMuPDF)."
        ) from exc
