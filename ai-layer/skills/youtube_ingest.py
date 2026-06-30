"""YouTube transcript ingestion. Optional dependencies: yt-dlp + faster-whisper.

Audio download + speech-to-text are heavy and network/model dependent, so this module only declares
the capability and raises an actionable error when the optional stack is absent.
"""
from __future__ import annotations


class OptionalDependencyError(RuntimeError):
    pass


def available() -> bool:
    try:
        import yt_dlp  # type: ignore # noqa: F401
        import faster_whisper  # type: ignore # noqa: F401

        return True
    except Exception:  # noqa: BLE001
        return False


def transcribe(url: str) -> str:
    if not available():
        raise OptionalDependencyError(
            "YouTube intake requires 'yt-dlp' and 'faster-whisper' "
            "(pip install yt-dlp faster-whisper)."
        )
    import tempfile

    import yt_dlp  # type: ignore
    from faster_whisper import WhisperModel  # type: ignore

    with tempfile.TemporaryDirectory() as tmp:
        out = f"{tmp}/audio.%(ext)s"
        opts = {"format": "bestaudio/best", "outtmpl": out, "quiet": True}
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=True)
            audio_path = ydl.prepare_filename(info)
        model = WhisperModel("base", device="cpu", compute_type="int8")
        segments, _ = model.transcribe(audio_path)
        return " ".join(seg.text.strip() for seg in segments)
