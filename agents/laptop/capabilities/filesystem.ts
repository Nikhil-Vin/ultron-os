/**
 * Filesystem capability (Section 11.7). Reads/writes/watches are confined to allow-listed root
 * directories (ULTRON_FS_ROOTS, comma-separated). Any path that escapes a root is rejected — a path
 * traversal guard. Writes are HIGH, deletes are CRITICAL (enforced by the daemon's approval check).
 */
import { watch } from "chokidar";
import { readFile, writeFile } from "node:fs/promises";
import { resolve, sep } from "node:path";

const ROOTS = (process.env.ULTRON_FS_ROOTS ?? "")
  .split(",")
  .map((r) => r.trim())
  .filter(Boolean)
  .map((r) => resolve(r));

function assertAllowed(path: string): string {
  const abs = resolve(path);
  const ok = ROOTS.some((root) => abs === root || abs.startsWith(root + sep));
  if (!ok) {
    throw new Error(`Path ${abs} is outside the allowed roots (${ROOTS.join(", ") || "none configured"})`);
  }
  return abs;
}

export async function readFileSafe(path: string): Promise<string> {
  return readFile(assertAllowed(path), "utf-8");
}

export async function writeFileSafe(path: string, content: string): Promise<void> {
  await writeFile(assertAllowed(path), content, "utf-8");
}

export function watchDir(path: string, onChange: (event: string, file: string) => void): () => void {
  const root = assertAllowed(path);
  const watcher = watch(root, { ignoreInitial: true, depth: 4 });
  watcher.on("all", (event, file) => onChange(event, file));
  return () => void watcher.close();
}

export function allowedRoots(): string[] {
  return [...ROOTS];
}
