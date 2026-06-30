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
    <div className="space-y-3">
      <h2 className="font-mono text-[11px] tracking-[0.25em] text-[#00e5ff]">CONNECTED DEVICES</h2>
      <ul className="space-y-1">
        {devices.length === 0 && <li className="font-mono text-[11px] text-[#5f8fae]">No devices connected. Run the Android/laptop agent.</li>}
        {devices.map((d) => (
          <li key={d.id} className="flex items-center justify-between rounded bg-black/40 px-3 py-2 font-mono text-[11px]">
            <span className="text-[#bfefff]">{d.type === "android" ? "📱" : "💻"} {d.id}</span>
            <span style={{ color: d.online ? "#34d399" : "#5f8fae" }}>{d.online ? "ONLINE" : "OFFLINE"}</span>
          </li>
        ))}
      </ul>
      <div className="flex gap-2">
        <input value={cmd} onChange={(e) => setCmd(e.target.value)} onKeyDown={(e) => e.key === "Enter" && send()}
          placeholder='e.g. "open YouTube on my phone"'
          className="flex-1 rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none focus:border-[#00e5ff]" />
        <button onClick={send} className="rounded border border-[#00e5ff]/40 bg-[#00e5ff]/10 px-4 py-2 font-mono text-[11px] tracking-widest text-[#00e5ff] hover:bg-[#00e5ff] hover:text-black">SEND</button>
      </div>
      {out && <p className="font-mono text-[11px] text-[#9fe8ff]">{out}</p>}
    </div>
  );
}
