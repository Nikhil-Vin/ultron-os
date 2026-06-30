"""Code generation (ai-layer counterpart, Section 9.2).

Parses an LLM's file-block output (===FILE: path===…===END===), writes the project to a sandboxed
output dir, and opens VS Code. The Java CodeGeneratorService is the primary path; this exists for
Python-driven generation (e.g. when the model/agent runs in the ai-layer) with the same contract.
"""
from __future__ import annotations

import os
import re
import subprocess
from pathlib import Path
from typing import List

FILE_BLOCK = re.compile(r"===FILE:\s*(.+?)===\s*\n(.*?)\n===END===", re.DOTALL)
OUTPUT_ROOT = Path(os.getenv("ULTRON_CODE_OUTPUT", str(Path.home() / "ultron-output")))


def write_project(llm_output: str, project_name: str) -> List[str]:
    """Write all file blocks from `llm_output` into OUTPUT_ROOT/<project_name> (path-escape safe)."""
    safe = re.sub(r"[^a-zA-Z0-9._-]", "-", project_name or "project")
    root = (OUTPUT_ROOT / safe).resolve()
    root.mkdir(parents=True, exist_ok=True)
    written: List[str] = []
    for m in FILE_BLOCK.finditer(llm_output):
        rel = m.group(1).strip().replace("\\", "/")
        target = (root / rel).resolve()
        if not str(target).startswith(str(root)):
            continue  # refuse path traversal
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(m.group(2), encoding="utf-8")
        written.append(str(target))
    return written


def open_vscode(path: str) -> None:
    try:
        if os.name == "nt":
            subprocess.Popen(["cmd", "/c", "code", path])
        else:
            subprocess.Popen(["code", path])
    except Exception:  # noqa: BLE001
        pass


def build_prompt(requirement: str, language: str, grounding: str = "") -> str:
    """Prompt that instructs the model to emit only delimited file blocks."""
    return (
        f"Generate a complete, working {language} project for:\n{requirement}\n\n"
        f"Follow these patterns from my knowledge base where relevant:\n{grounding or '(none)'}\n\n"
        "Output ONLY files, each delimited EXACTLY as:\n"
        "===FILE: relative/path===\n<contents>\n===END===\n"
        "Include all source, config, and a README. No prose outside the file blocks."
    )


if __name__ == "__main__":
    import sys
    name = sys.argv[1] if len(sys.argv) > 1 else "demo"
    data = sys.stdin.read()
    files = write_project(data, name)
    print(f"wrote {len(files)} files")
    if files:
        open_vscode(str(Path(files[0]).parent))
