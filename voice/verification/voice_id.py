"""Speaker verification via Resemblyzer (Section 11.6 / 14).

Enrolls the owner's voiceprint from one or more samples and verifies a live utterance by cosine
similarity. The resulting score is sent to the Java VoiceIdGate, which makes the pass/fail decision
against the configured threshold for CRITICAL actions. Language-independent.
"""
from __future__ import annotations

import json
import os
from pathlib import Path
from typing import List

import numpy as np

# Resemblyzer is heavy (torch). Imported lazily so the module loads even before install.
try:
    from resemblyzer import VoiceEncoder, preprocess_wav

    _ENCODER = None

    def _encoder() -> "VoiceEncoder":
        global _ENCODER
        if _ENCODER is None:
            _ENCODER = VoiceEncoder()
        return _ENCODER

    _AVAILABLE = True
except Exception:  # noqa: BLE001
    _AVAILABLE = False


ENROLL_PATH = Path(os.getenv("ULTRON_VOICEPRINT", "models/owner_voiceprint.json"))


def available() -> bool:
    return _AVAILABLE


def enroll(wav_paths: List[str], out_path: Path = ENROLL_PATH) -> List[float]:
    """Compute and persist the owner's voiceprint from MULTIPLE samples (Phase 5 hardening).

    Robustness: each sample is embedded; samples whose cosine to the running mean is an outlier
    (likely noise/another speaker) are dropped, then the cleaned set is averaged. Enrolling with
    3-5 varied samples (quiet, normal, slightly noisy) materially improves verification accuracy.
    """
    if not _AVAILABLE:
        raise RuntimeError("Resemblyzer not installed; run pip install resemblyzer")
    if len(wav_paths) < 1:
        raise ValueError("provide at least one enrollment sample (3-5 recommended)")

    embeds = []
    for p in wav_paths:
        try:
            embeds.append(_encoder().embed_utterance(preprocess_wav(p)))
        except Exception:  # noqa: BLE001
            print(f"[voice_id] skipped unreadable sample: {p}")
    if not embeds:
        raise RuntimeError("no usable enrollment samples")

    arr = np.array(embeds)
    mean = np.mean(arr, axis=0)
    # Outlier rejection: keep samples within 0.15 cosine of the mean (when we have enough).
    if len(arr) >= 3:
        sims = [float(np.dot(mean, e) / (np.linalg.norm(mean) * np.linalg.norm(e) + 1e-9)) for e in arr]
        keep = [e for e, s in zip(arr, sims) if s >= (max(sims) - 0.15)]
        if keep:
            arr = np.array(keep)
    voiceprint = np.mean(arr, axis=0)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps({
        "voiceprint": voiceprint.tolist(),
        "samples_used": int(len(arr)),
        "samples_provided": int(len(wav_paths)),
    }))
    print(f"[voice_id] enrolled from {len(arr)}/{len(wav_paths)} samples → {out_path}")
    return voiceprint.tolist()


def _load_voiceprint(path: Path = ENROLL_PATH):
    if not path.exists():
        return None
    raw = json.loads(path.read_text())
    vec = raw.get("voiceprint", raw) if isinstance(raw, dict) else raw
    return np.array(vec, dtype=np.float32)


def verify(wav_or_samples, voiceprint_path: Path = ENROLL_PATH) -> float:
    """Return cosine similarity in [0,1] between a live utterance and the enrolled voiceprint.

    Returns -1.0 when no voiceprint is enrolled or the encoder is unavailable, so the Java gate
    treats it as a hard fail (secure default).
    """
    if not _AVAILABLE:
        return -1.0
    enrolled = _load_voiceprint(voiceprint_path)
    if enrolled is None:
        return -1.0
    wav = preprocess_wav(wav_or_samples) if isinstance(wav_or_samples, str) else wav_or_samples
    live = _encoder().embed_utterance(wav)
    sim = float(np.dot(enrolled, live) / (np.linalg.norm(enrolled) * np.linalg.norm(live) + 1e-9))
    # Map cosine [-1,1] → [0,1] to match the Java threshold convention.
    return max(0.0, min(1.0, (sim + 1.0) / 2.0))


def is_enrolled(voiceprint_path: Path = ENROLL_PATH) -> bool:
    return voiceprint_path.exists()


if __name__ == "__main__":
    import argparse

    ap = argparse.ArgumentParser(description="Ultron voice biometric enrollment/verification")
    sub = ap.add_subparsers(dest="cmd", required=True)
    e = sub.add_parser("enroll", help="enroll the owner from multiple wav samples")
    e.add_argument("samples", nargs="+", help="3-5 wav files (varied conditions recommended)")
    v = sub.add_parser("verify", help="verify a wav against the enrolled voiceprint")
    v.add_argument("sample")
    args = ap.parse_args()

    if not available():
        raise SystemExit("Resemblyzer not installed: pip install resemblyzer")
    if args.cmd == "enroll":
        enroll(args.samples)
    elif args.cmd == "verify":
        print(f"similarity = {verify(args.sample):.3f}  (Java VoiceIdGate decides pass/fail vs threshold)")
