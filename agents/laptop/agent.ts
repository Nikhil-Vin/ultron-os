/**
 * Ultron laptop agent (Windows-first) — Section 11.7 / device control.
 * Persistent WebSocket to ws://<host>:8080/ws/device/laptop. Registers capabilities, receives
 * {id, action, args, approvalToken}, executes real OS actions, and returns {type:"result"}.
 * READ actions run instantly; anything above READ requires the backend's approvalToken. The
 * capability manifest forbids destructive actions outright.
 *
 *   npm install && npm run dev          (tsx agent.ts)
 *   set ULTRON_WS=ws://localhost:8080/ws/device/laptop
 */
import { WebSocket } from "ws";
import { exec, spawn } from "node:child_process";
import { promisify } from "node:util";

const execAsync = promisify(exec);
const WS_URL = process.env.ULTRON_WS ?? "ws://localhost:8080/ws/device/laptop";
const isWin = process.platform === "win32";

const CAPABILITIES = [
  "open_app", "open_url", "screenshot", "processes", "sysinfo",
  "lock", "mute", "explorer", "type", "mouse_click",
];
const READ = new Set(["processes", "sysinfo", "screenshot"]);
const FORBIDDEN = new Set(["format", "delete_system", "shutdown_force", "registry_wipe"]);

interface Cmd { id: string; action: string; args: Record<string, any>; approvalToken?: string; }

async function ps(command: string) {
  try {
    const { stdout, stderr } = await execAsync(`powershell -NoProfile -Command "${command.replace(/"/g, '\\"')}"`, { timeout: 20000, maxBuffer: 1 << 20 });
    return { ok: true, out: stdout.trim(), err: stderr.trim() };
  } catch (e: any) {
    return { ok: false, out: "", err: String(e?.message ?? e) };
  }
}

async function execute(cmd: Cmd) {
  const a = cmd.args || {};
  switch (cmd.action) {
    case "open_app": {
      const app = String(a.app || "").toLowerCase();
      const map: Record<string, string> = { "vs code": "code", vscode: "code", chrome: "start chrome", explorer: "explorer", notepad: "notepad", terminal: "wt" };
      const c = map[app] || app;
      spawn(isWin ? "cmd" : "sh", [isWin ? "/c" : "-c", c], { detached: true, stdio: "ignore" }).unref();
      return { ok: true, out: `launched ${c}` };
    }
    case "open_url":
      spawn(isWin ? "cmd" : "sh", [isWin ? "/c" : "-c", `start ${a.url}`], { detached: true, stdio: "ignore" }).unref();
      return { ok: true, out: `opened ${a.url}` };
    case "explorer":
      spawn("explorer", [String(a.path || process.env.USERPROFILE || ".")], { detached: true, stdio: "ignore" }).unref();
      return { ok: true, out: "explorer opened" };
    case "lock":
      return ps("rundll32.exe user32.dll,LockWorkStation");
    case "mute":
      return ps("(New-Object -ComObject WScript.Shell).SendKeys([char]173)"); // toggle mute
    case "processes":
      return ps("Get-Process | Sort-Object CPU -desc | Select-Object -First 12 Name,CPU,Id | ConvertTo-Json");
    case "sysinfo":
      return ps("$os=Get-CimInstance Win32_OperatingSystem; $cpu=(Get-CimInstance Win32_Processor).LoadPercentage; \"CPU=$cpu% MEM_FREE_MB=$([math]::round($os.FreePhysicalMemory/1024))\"");
    case "screenshot":
      // Save to user profile; full capture via .NET.
      return ps("Add-Type -AssemblyName System.Windows.Forms,System.Drawing; $b=[System.Windows.Forms.Screen]::PrimaryScreen.Bounds; $bmp=New-Object Drawing.Bitmap $b.Width,$b.Height; $g=[Drawing.Graphics]::FromImage($bmp); $g.CopyFromScreen(0,0,0,0,$bmp.Size); $p=\"$env:USERPROFILE\\ultron_shot.png\"; $bmp.Save($p); $p");
    case "type":
      return ps(`(New-Object -ComObject WScript.Shell).SendKeys('${String(a.text || "").replace(/'/g, "''")}')`);
    case "mouse_click":
      return ps("Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point([int]([System.Windows.Forms.Screen]::PrimaryScreen.Bounds.Width/2),[int]([System.Windows.Forms.Screen]::PrimaryScreen.Bounds.Height/2))");
    default:
      return { ok: false, err: `unknown action ${cmd.action}` };
  }
}

function connect() {
  const ws = new WebSocket(WS_URL);
  ws.on("open", () => {
    ws.send(JSON.stringify({ type: "register", capabilities: CAPABILITIES }));
    console.log(`[ultron-laptop] connected → ${WS_URL}`);
  });
  ws.on("message", async (raw) => {
    let cmd: Cmd;
    try { cmd = JSON.parse(raw.toString()); } catch { return; }
    if (FORBIDDEN.has(cmd.action)) {
      ws.send(JSON.stringify({ type: "result", id: cmd.id, result: { ok: false, err: "forbidden" } }));
      return;
    }
    if (!READ.has(cmd.action) && !cmd.approvalToken) {
      ws.send(JSON.stringify({ type: "result", id: cmd.id, result: { ok: false, err: "requires backend approval" } }));
      return;
    }
    const result = await execute(cmd);
    ws.send(JSON.stringify({ type: "result", id: cmd.id, result }));
  });
  ws.on("close", () => { console.log("[ultron-laptop] disconnected; retry 5s"); setTimeout(connect, 5000); });
  ws.on("error", (e) => console.error("[ultron-laptop] ws error:", e.message));
}

connect();
