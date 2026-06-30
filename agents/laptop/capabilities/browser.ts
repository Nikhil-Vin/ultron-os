/**
 * Browser automation capability (Section 11.7) via Playwright. `readPage` (extract text) is READ;
 * `actOnPage` (click/type/submit) is HIGH and only runs after backend approval.
 */
import { chromium, type Browser } from "playwright";

let browser: Browser | null = null;

async function getBrowser(): Promise<Browser> {
  if (!browser) {
    browser = await chromium.launch({ headless: true });
  }
  return browser;
}

/** READ: open a URL and return its visible text. */
export async function readPage(url: string): Promise<{ url: string; title: string; text: string }> {
  const b = await getBrowser();
  const page = await b.newPage();
  try {
    await page.goto(url, { waitUntil: "domcontentloaded", timeout: 30_000 });
    const title = await page.title();
    const text = await page.evaluate(() => document.body?.innerText ?? "");
    return { url, title, text: text.slice(0, 20_000) };
  } finally {
    await page.close();
  }
}

export type BrowserStep =
  | { kind: "click"; selector: string }
  | { kind: "type"; selector: string; value: string }
  | { kind: "goto"; url: string };

/** HIGH: perform a sequence of actions on a page. Caller must have an approval token. */
export async function actOnPage(url: string, steps: BrowserStep[]): Promise<{ ok: boolean; finalUrl: string }> {
  const b = await getBrowser();
  const page = await b.newPage();
  try {
    await page.goto(url, { waitUntil: "domcontentloaded", timeout: 30_000 });
    for (const step of steps) {
      if (step.kind === "click") await page.click(step.selector, { timeout: 10_000 });
      else if (step.kind === "type") await page.fill(step.selector, step.value, { timeout: 10_000 });
      else if (step.kind === "goto") await page.goto(step.url, { waitUntil: "domcontentloaded" });
    }
    return { ok: true, finalUrl: page.url() };
  } finally {
    await page.close();
  }
}

export async function shutdown(): Promise<void> {
  await browser?.close();
  browser = null;
}
