/**
 * OS automation capability (Section 11.7). Cross-platform command runner: AppleScript on macOS,
 * AutoHotkey/PowerShell on Windows, shell on Linux. This is HIGH risk — the daemon only calls it
 * AFTER an approval token is verified. Uses argument arrays (no string interpolation) to avoid
 * command injection.
 */
import { execFile } from "node:child_process";
import { platform } from "node:os";
import { promisify } from "node:util";

const run = promisify(execFile);

export interface OsResult {
  ok: boolean;
  stdout: string;
  stderr: string;
}

/** Run a command with explicit args (never a single interpolated string). */
export async function runCommand(command: string, args: string[] = []): Promise<OsResult> {
  try {
    const { stdout, stderr } = await run(command, args, { timeout: 30_000, maxBuffer: 1024 * 1024 });
    return { ok: true, stdout, stderr };
  } catch (e: any) {
    return { ok: false, stdout: e?.stdout ?? "", stderr: e?.stderr ?? String(e) };
  }
}

/** Run an AppleScript (macOS) / show an error elsewhere. */
export async function runAppleScript(script: string): Promise<OsResult> {
  if (platform() !== "darwin") {
    return { ok: false, stdout: "", stderr: "AppleScript only available on macOS" };
  }
  return runCommand("osascript", ["-e", script]);
}

/** Run a PowerShell command (Windows). */
export async function runPowerShell(script: string): Promise<OsResult> {
  if (platform() !== "win32") {
    return { ok: false, stdout: "", stderr: "PowerShell path is Windows-only here" };
  }
  return runCommand("powershell.exe", ["-NoProfile", "-NonInteractive", "-Command", script]);
}
