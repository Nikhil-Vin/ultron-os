/**
 * Screen capability (Section 11.7). Captures a screenshot and runs local OCR (tesseract.js). READ
 * level — stays on-device; nothing is uploaded. Useful for "what's on my screen" context.
 */
import screenshot from "screenshot-desktop";
import { createWorker } from "tesseract.js";

export async function capture(): Promise<Buffer> {
  return screenshot({ format: "png" });
}

export async function captureAndOcr(): Promise<{ text: string; width: number }> {
  const img = await screenshot({ format: "png" });
  const worker = await createWorker("eng");
  try {
    const { data } = await worker.recognize(img);
    return { text: data.text.trim(), width: (data as any).width ?? 0 };
  } finally {
    await worker.terminate();
  }
}
