"""PEFT LoRA adapter training (Phase 2/5 — optional deep specialization).

Fine-tunes a base local LLM into a domain adapter from a corpus of MY data (e.g. all chunks of an
ingested skill, or my trade journal). Triggered by /api/skills/{tag}/fine-tune. Requires
transformers + peft + datasets + torch (+ bitsandbytes for 4-bit on consumer GPUs).

  python lora_trainer.py --corpus skill_chunks.txt --adapter models/adapters/trading_psychology
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path

BASE_MODEL = os.getenv("ULTRON_LORA_BASE", "meta-llama/Llama-3.1-8B-Instruct")


def train(corpus_path: str, adapter_out: str, epochs: int = 3, four_bit: bool = True) -> str:
    text = Path(corpus_path).read_text(encoding="utf-8", errors="replace")
    blocks = [b.strip() for b in text.split("\n\n") if b.strip()]
    print(f"[lora_trainer] {len(blocks)} training blocks, base={BASE_MODEL}, 4bit={four_bit}")

    from datasets import Dataset  # type: ignore
    from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training  # type: ignore
    from transformers import (AutoModelForCausalLM, AutoTokenizer,  # type: ignore
                              DataCollatorForLanguageModeling, Trainer, TrainingArguments)

    tok = AutoTokenizer.from_pretrained(BASE_MODEL)
    if tok.pad_token is None:
        tok.pad_token = tok.eos_token

    model_kwargs = {}
    if four_bit:
        try:
            from transformers import BitsAndBytesConfig  # type: ignore
            import torch  # type: ignore
            model_kwargs["quantization_config"] = BitsAndBytesConfig(
                load_in_4bit=True, bnb_4bit_compute_dtype=torch.float16, bnb_4bit_quant_type="nf4")
        except Exception:  # noqa: BLE001
            print("[lora_trainer] bitsandbytes unavailable; training in full precision")

    model = AutoModelForCausalLM.from_pretrained(BASE_MODEL, **model_kwargs)
    if four_bit:
        model = prepare_model_for_kbit_training(model)
    model = get_peft_model(model, LoraConfig(
        r=16, lora_alpha=32, lora_dropout=0.05, task_type="CAUSAL_LM",
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj"]))

    ds = Dataset.from_list([{"text": b} for b in blocks]).map(
        lambda e: tok(e["text"], truncation=True, max_length=512), remove_columns=["text"])

    trainer = Trainer(
        model=model,
        args=TrainingArguments(output_dir=adapter_out, num_train_epochs=epochs,
                               per_device_train_batch_size=1, gradient_accumulation_steps=8,
                               learning_rate=2e-4, fp16=four_bit, logging_steps=10,
                               save_strategy="epoch", report_to=[]),
        train_dataset=ds,
        data_collator=DataCollatorForLanguageModeling(tok, mlm=False),
    )
    trainer.train()
    model.save_pretrained(adapter_out)
    tok.save_pretrained(adapter_out)
    print(f"[lora_trainer] saved adapter → {adapter_out}")
    return adapter_out


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--corpus", required=True)
    ap.add_argument("--adapter", required=True)
    ap.add_argument("--epochs", type=int, default=3)
    ap.add_argument("--no-4bit", action="store_true")
    a = ap.parse_args()
    train(a.corpus, a.adapter, a.epochs, not a.no_4bit)
