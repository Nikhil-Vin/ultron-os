import { useEffect, useState } from "react";
import { api } from "../../lib/api";

export default function DevicesView() {
  const [devices, setDevices] = useState<{ id: string; type: string; online: boolean; capabilities: string[] }[]>([]);
  const [cmd, setCmd] = useState("");
  const [out, setOut] = useState<string | null>(null);

  async function load() { setDevices(await api.devices().catch(() => [])); }
  useEffect(() => { load(); }, []);

  async function send() {
    if (!cmd.trim()) return;
    const r = await api.deviceCommand(cmd).catch((e) => ({ message: String(e) }));
    setOut(r.message || (r.executed ? `Done — ${r.action} on ${r.target}` : "no match / blocked"));
    setCmd("");
  }

  return (
    <div className="space-y-4">
      <h2 className="font-mono text-[11px] tracking-[0.25em] text-[#00e5ff]">CONNECTED DEVICES</h2>
      
      {devices.length === 0 && (
        <div className="rounded-lg border border-amber-500/30 bg-amber-950/20 p-4">
          <div className="mb-2 flex items-center gap-2">
            <span className="text-amber-500">ℹ</span>
            <span className="font-mono text-sm text-amber-200">No devices connected</span>
          </div>
          <div className="space-y-3 text-xs text-amber-200/80">
            <div>
              <div className="mb-1 font-mono text-amber-300">Laptop Agent:</div>
              <pre className="rounded bg-black/40 p-2 font-mono text-[11px] text-gray-300">
cd agents/laptop
npm install
npx tsx agent.ts</pre>
              <p className="mt-1 text-[10px] text-amber-200/60">
                Allows Ultron to control your laptop (open apps, run commands, etc.)
              </p>
            </div>
            
            <div>
              <div className="mb-1 font-mono text-amber-300">Android Agent:</div>
              <pre className="rounded bg-black/40 p-2 font-mono text-[11px] text-gray-300">
cd agents/phone
npm install
npx tsx agent.ts</pre>
              <p className="mt-1 text-[10px] text-amber-200/60">
                Controls your Android phone (requires ADB and USB debugging enabled)
              </p>
            </div>
            
            <p className="text-[10px] text-amber-200/60">
              Once connected, devices appear above and you can send natural language commands like 
              "open notepad", "lock my phone", or "take a screenshot".
            </p>
          </div>
        </div>
      )}
      
      <ul className="space-y-1">
        {devices.map((d) => (
          <li key={d.id} className="flex items-center justify-between rounded bg-black/40 px-3 py-2 font-mono text-[11px]">
            <span className="text-[#bfefff]">{d.type === "android" ? "📱" : "💻"} {d.id}</span>
            <div className="flex items-center gap-3">
              <span className="text-[10px] text-gray-500">
                {d.capabilities.join(", ")}
              </span>
              <span style={{ color: d.online ? "#34d399" : "#5f8fae" }}>
                {d.online ? "ONLINE" : "OFFLINE"}
              </span>
            </div>
          </li>
        ))}
      </ul>
      
      {devices.length > 0 && (
        <>
          <div className="flex gap-2">
            <input value={cmd} onChange={(e) => setCmd(e.target.value)} onKeyDown={(e) => e.key === "Enter" && send()}
              placeholder='e.g. "open YouTube on my phone"'
              className="flex-1 rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none focus:border-[#00e5ff]" />
            <button onClick={send} className="rounded border border-[#00e5ff]/40 bg-[#00e5ff]/10 px-4 py-2 font-mono text-[11px] tracking-widest text-[#00e5ff] hover:bg-[#00e5ff] hover:text-black">SEND</button>
          </div>
          {out && <pre className="whitespace-pre-wrap rounded bg-black/40 p-3 font-mono text-[11px] text-[#9fe8ff]">{out}</pre>}
        </>
      )}
    </div>
  );
}
