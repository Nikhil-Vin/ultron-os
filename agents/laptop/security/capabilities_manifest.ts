/**
 * Capability manifest (Section 11.7). The single declaration of what this device agent is allowed
 * to do, and at what risk tier. The daemon refuses any action not listed here, and any action above
 * READ must carry a backend approval token (the ApprovalGate + VoiceIdGate decision). Nothing is
 * auto-wired to fire — this is the device-side half of the human-in-the-loop moat.
 */

export type RiskLevel = "READ" | "LOW" | "HIGH" | "CRITICAL";

export interface Capability {
  action: string;
  risk: RiskLevel;
  description: string;
}

export const CAPABILITIES: Capability[] = [
  // READ — safe, run freely
  { action: "system.info", risk: "READ", description: "CPU/RAM/disk stats" },
  { action: "screen.capture", risk: "READ", description: "Screenshot + OCR (local only)" },
  { action: "fs.read", risk: "READ", description: "Read a file under an allowed directory" },
  { action: "fs.watch", risk: "READ", description: "Watch a directory for changes" },
  { action: "browser.read", risk: "READ", description: "Open a page and extract text" },

  // LOW — logged + notified
  { action: "fs.writeDraft", risk: "LOW", description: "Write to a scratch/drafts directory only" },

  // HIGH — require explicit approval token
  { action: "os.run", risk: "HIGH", description: "Run a shell/AppleScript/AutoHotkey command" },
  { action: "fs.write", risk: "HIGH", description: "Write/modify an arbitrary file" },
  { action: "browser.act", risk: "HIGH", description: "Click/type/submit on a page" },

  // CRITICAL — approval + voice biometric
  { action: "fs.delete", risk: "CRITICAL", description: "Delete files" },
];

/** Forbidden outright — never executed regardless of approval. */
export const FORBIDDEN: string[] = [
  "os.format",
  "fs.deleteSystem",
  "net.exfiltrate",
  "creds.read",
];

export function riskOf(action: string): RiskLevel | null {
  const cap = CAPABILITIES.find((c) => c.action === action);
  return cap ? cap.risk : null;
}

export function isForbidden(action: string): boolean {
  return FORBIDDEN.includes(action);
}
