"""Fine-tune the Function Gemma routing model (Phase 5).

Trains a tiny think/direct/tool classifier on ~200 labeled examples (yours + the seed below) using
PEFT/LoRA so it runs on consumer hardware. Requires: transformers, peft, datasets, torch.

  python train_router.py --data routing_examples.jsonl --out models/function_gemma_router

Each line of the JSONL: {"prompt": "...", "label": "THINK|DIRECT|TOOL:<name>"}.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path

BASE_MODEL = os.getenv("ULTRON_ROUTER_BASE", "google/gemma-2-2b-it")

SEED_EXAMPLES = [
    {"prompt": "plan my week around the product launch", "label": "THINK"},
    {"prompt": "what's my deploy command", "label": "DIRECT"},
    {"prompt": "remember the wifi password is hunter2", "label": "TOOL:capture_memory"},
    {"prompt": "give me a signal for NIFTY with RSI 28", "label": "TOOL:trading_signal"},
    {"prompt": "compare mean-reversion vs momentum for my account", "label": "THINK"},
    {"prompt": "who did I meet last tuesday", "label": "DIRECT"},
]


def load_examples(path: str | None) -> list[dict]:
    rows = list(SEED_EXAMPLES)
    if path and Path(path).exists():
        for line in Path(path).read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line:
                rows.append(json.loads(line))
    return rows


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default=None)
    ap.add_argument("--out", default="models/function_gemma_router")
    ap.add_argument("--epochs", type=int, default=3)
    args = ap.parse_args()

    examples = load_examples(args.data)
    print(f"[train_router] {len(examples)} examples, base={BASE_MODEL}")

    from datasets import Dataset  # type: ignore
    from peft import LoraConfig, get_peft_model  # type: ignore
    from transformers import (AutoModelForCausalLM, AutoTokenizer,  # type: ignore
                              DataCollatorForLanguageModeling, Trainer, TrainingArguments)

    tok = AutoTokenizer.from_pretrained(BASE_MODEL)
    if tok.pad_token is None:
        tok.pad_token = tok.eos_token

    def fmt(ex: dict) -> dict:
        text = f"Classify the request as THINK, DIRECT, or TOOL:<name>. Request: {ex['prompt']}\nLabel: {ex['label']}"
        return tok(text, truncation=True, max_length=128)

    ds = Dataset.from_list(examples).map(fmt, remove_columns=["prompt", "label"])
    model = AutoModelForCausalLM.from_pretrained(BASE_MODEL)
    model = get_peft_model(model, LoraConfig(
        r=8, lora_alpha=16, lora_dropout=0.05, task_type="CAUSAL_LM",
        target_modules=["q_proj", "v_proj"]))

    trainer = Trainer(
        model=model,
        args=TrainingArguments(output_dir=args.out, num_train_epochs=args.epochs,
                               per_device_train_batch_size=2, learning_rate=2e-4,
                               logging_steps=10, save_strategy="epoch", report_to=[]),
        train_dataset=ds,
        data_collator=DataCollatorForLanguageModeling(tok, mlm=False),
    )
    trainer.train()
    model.save_pretrained(args.out)
    tok.save_pretrained(args.out)
    print(f"[train_router] saved adapter to {args.out}")


if __name__ == "__main__":
    main()
