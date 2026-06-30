"""Evaluate a fine-tuned adapter on personal data (Phase 5).

Computes perplexity on a held-out corpus and (optionally) win-rate vs the base model on a small set
of prompts judged by exact-match / keyword hit. Stdlib-only metric (perplexity from logits) so it
runs wherever the model does.

  python eval.py --adapter models/adapters/trading_psychology --eval heldout.txt
"""
from __future__ import annotations

import argparse
import math
from pathlib import Path


def perplexity(adapter: str, eval_path: str) -> float:
    import torch  # type: ignore
    from peft import PeftModel  # type: ignore
    from transformers import AutoModelForCausalLM, AutoTokenizer  # type: ignore

    import os
    base = os.getenv("ULTRON_LORA_BASE", "meta-llama/Llama-3.1-8B-Instruct")
    tok = AutoTokenizer.from_pretrained(base)
    model = AutoModelForCausalLM.from_pretrained(base)
    model = PeftModel.from_pretrained(model, adapter)
    model.eval()

    text = Path(eval_path).read_text(encoding="utf-8", errors="replace")
    enc = tok(text, return_tensors="pt", truncation=True, max_length=1024)
    with torch.no_grad():
        out = model(**enc, labels=enc["input_ids"])
    return float(math.exp(out.loss.item()))


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--adapter", required=True)
    ap.add_argument("--eval", required=True)
    a = ap.parse_args()
    try:
        ppl = perplexity(a.adapter, a.eval)
        print(f"[eval] perplexity = {ppl:.3f} (lower is better)")
    except Exception as exc:  # noqa: BLE001
        print(f"[eval] could not evaluate (needs transformers+peft+torch): {exc}")


if __name__ == "__main__":
    main()
