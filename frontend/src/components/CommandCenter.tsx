import { useEffect, useRef, useState } from "react";
import { api, type Health } from "../lib/api";
import NeuralSphere from "./jarvis/NeuralSphere";
import VoiceConsole from "./jarvis/VoiceConsole";
import FeedPanel from "./jarvis/FeedPanel";

const BOOT_LINES = [
  "BOOTING ULTRON CORE v4.2 ...",
  "SPEECH AND AUDIO SYNTACTIC CORE ... ONLINE",
  "LOADING HIGH-FREQUENCY NLP TOKENIZER ... READY",
  "INITIALIZING MULTIMODAL CONTEXT STREAM ... ACTIVE",
  "MOUNTING MEMORY MATRIX (pgvector) ... OK",
  "ARMING GOVERNANCE GATE (human-in-the-loop) ... SECURE",
  "NEURAL MESH SYNCHRONIZED ... ALIVE",
];

interface FeedItem { ts: number; tag: string; text: string; }

export default function CommandCenter() {
  const [health, setHealth] = useState<Health | null>(null);
  const [mode, setMode] = useState<"default" | "trading" | "research">("default");
  const [thinking, setThinking] = useState(false);
  const [boot, setBoot] = useState<string[]>([]);
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [counts, setCounts] = useState({ memories: 0, skills: 0 });
  const [connections, setConnections] = useState(135000);
  const [clock, setClock] = useState("");
  const [vitals, setVitals] = useState({ cpu: 0, mem: 0 });
  const [uptime, setUptime] = useState(0);
  const [feedOpen, setFeedOpen] = useState(false);
  const [providers, setProviders] = useState<{ name: string; available: boolean; active: boolean }[]>([]);
  const [pinned, setPinned] = useState("auto");
  const [devices, setDevices] = useState<{ type: string; online: boolean }[]>([]);
  const bootIdx = useRef(0);

  // boot typing
  useEffect(() => {
    const id = setInterval(() => {
      if (bootIdx.current >= BOOT_LINES.length) { clearInterval(id); return; }
      setBoot((p) => [...p, BOOT_LINES[bootIdx.current++]]);
    }, 550);
    return () => clearInterval(id);
  }, []);

  // clock + ticking counter + drifting vitals
  useEffect(() => {
    const id = setInterval(() => {
      const n = new Date();
      setClock([n.getHours(), n.getMinutes(), n.getSeconds()].map((x) => String(x).padStart(2, "0")).join(":"));
    }, 1000);
    return () => clearInterval(id);
  }, []);

  // real data
  useEffect(() => {
    let alive = true;
    async function load() {
      try {
        const [h, mem, skills, signals] = await Promise.all([
          api.health().catch(() => null),
          api.recall("").catch(() => []),
          api.listSkills().catch(() => []),
          api.tradingSignals().catch(() => []),
        ]);
        if (!alive) return;
        if (h) setHealth(h);
        setCounts({ memories: mem.length, skills: skills.length });
        setConnections(135000 + mem.length * 137 + skills.length * 911 + (h ? h.workers.length * 4200 : 0));
        const items: FeedItem[] = [];
        for (const s of signals.slice(0, 6)) items.push({ ts: Date.parse(s.createdAt) || Date.now(), tag: "SIGNAL", text: `${s.signalType} ${s.instrument} · ${(s.confidence * 100).toFixed(0)}%` });
        for (const m of mem.slice(0, 6)) items.push({ ts: Date.parse(m.createdAt) || Date.now(), tag: m.type, text: m.content });
        items.sort((a, b) => b.ts - a.ts);
        setFeed(items.slice(0, 12));
        const bp = await api.brainProviders().catch(() => null);
        if (bp && alive) { setProviders(bp.providers); setPinned(bp.pinned); }
        const dv = await api.devices().catch(() => []);
        if (alive) setDevices(dv);
        const sys = await api.systemStats().catch(() => null);
        if (sys && alive) {
          setVitals({ cpu: sys.cpuPercent, mem: sys.heapPercent });
          setUptime(sys.uptimeSeconds);
        }
      } catch { /* ignore */ }
    }
    load();
    const id = setInterval(load, 8000);
    return () => { alive = false; clearInterval(id); };
  }, []);

  const accent = mode === "trading" ? "#ffb300" : mode === "research" ? "#9b5cff" : "#00e5ff";

  return (
    <div className="relative h-screen w-screen overflow-hidden bg-black text-[#cfe9ff]" style={{ ["--ac" as any]: accent }}>
      <NeuralSphere mode={mode} thinking={thinking} />

      {/* TOP LEFT — title + boot log */}
      <div className="absolute left-5 top-5 z-20 w-[340px]">
        <h1 className="font-sans text-2xl font-black tracking-[0.15em] txt-glow" style={{ color: accent }}>ULTRON OS</h1>
        <p className="font-mono text-[10px] tracking-[0.35em]" style={{ color: accent, opacity: 0.6 }}>v4.2 · GOLD_NEURAL_CORE</p>
        <div className="mt-3 glass glass-corner relative p-3 font-mono text-[10px] leading-relaxed text-[#8fd9ff]">
          {boot.map((l, i) => (
            <div key={i}><span className="opacity-40">›</span> {l}</div>
          ))}
          {boot.length >= BOOT_LINES.length && <div className="blink" style={{ color: accent }}>█ READY FOR INSTRUCTION</div>}
        </div>
      </div>

      {/* TOP RIGHT — clock + quick actions */}
      <div className="absolute right-5 top-5 z-20 flex flex-col items-end gap-3">
        <div className="font-mono text-3xl font-bold txt-glow" style={{ color: accent }}>{clock}</div>
        <div className="flex gap-1.5">
          {(["android", "laptop"] as const).map((dt) => {
            const on = devices.some((d) => d.type === dt && d.online);
            return (
              <span key={dt} className="glass px-2 py-1 font-mono text-[9px] tracking-widest"
                style={{ color: on ? "#34d399" : "#5f8fae" }}>
                {dt === "android" ? "PHONE" : "LAPTOP"}: {on ? "ONLINE" : "OFFLINE"}
              </span>
            );
          })}
        </div>
        <div className="glass px-2.5 py-1 font-mono text-[10px] tracking-widest" style={{ color: accent }}>
          BRAIN: {(health?.brain ?? "—").toUpperCase()}{health?.llmActive ? "·LLM" : ""}
        </div>
        <select
          value={pinned}
          onChange={async (e) => {
            const p = e.target.value;
            setPinned(p);
            await api.selectBrain(p).catch(() => {});
            const bp = await api.brainProviders().catch(() => null);
            if (bp) { setProviders(bp.providers); setHealth((h) => h && ({ ...h, brain: bp.active, llmActive: bp.llmActive })); }
          }}
          className="glass px-2 py-1 font-mono text-[10px] tracking-widest outline-none"
          style={{ color: accent, background: "rgba(4,8,14,0.8)" }}
          title="Pin a reasoning provider"
        >
          <option value="auto">AUTO</option>
          {providers.map((p) => (
            <option key={p.name} value={p.name} disabled={!p.available && p.name !== "heuristic" && p.name !== "ollama"}>
              {p.name.toUpperCase()}{p.available ? "" : " (no key)"}
            </option>
          ))}
        </select>
        <div className="flex flex-wrap justify-end gap-1.5">
          {[
            { k: "CORE", m: "default" as const },
            { k: "TRADING", m: "trading" as const },
            { k: "RESEARCH", m: "research" as const },
          ].map((b) => (
            <button
              key={b.k}
              onClick={() => setMode(b.m)}
              className={"glass px-2.5 py-1 font-mono text-[10px] tracking-widest transition-all " + (mode === b.m ? "txt-glow" : "opacity-60 hover:opacity-100")}
              style={{ color: accent, borderColor: mode === b.m ? accent : undefined }}
            >
              {b.k}
            </button>
          ))}
          {["TALK", "LINK", "ONLINE", "ALIVE"].map((k) => (
            <span key={k} className="glass px-2.5 py-1 font-mono text-[10px] tracking-widest" style={{ color: accent, opacity: 0.55 }}>{k}</span>
          ))}
          <button
            onClick={() => setFeedOpen(true)}
            className="glass px-2.5 py-1 font-mono text-[10px] tracking-widest hover:opacity-100"
            style={{ color: accent }}
          >
            ＋FEED
          </button>
        </div>
      </div>

      {/* LEFT PANEL — system vitals */}
      <div className="absolute left-5 top-1/2 z-20 w-[230px] -translate-y-1/2">
        <div className="glass glass-corner relative p-4">
          <h2 className="mb-3 font-mono text-[10px] tracking-[0.25em]" style={{ color: accent }}>SYSTEM_VITALS</h2>
          <div className="mb-4">
            <div className="font-sans text-4xl font-black text-white txt-glow">{counts.memories ? abbreviate(connections) : "135K"}</div>
            <div className="font-mono text-[9px] uppercase tracking-widest text-[#7fb8d8]">neural synapses</div>
          </div>
          <div className="grid grid-cols-2 gap-3 text-center">
            <Stat label="MEMORIES" value={counts.memories} accent={accent} />
            <Stat label="SKILLS" value={counts.skills} accent={accent} />
            <Stat label="WORKERS" value={health?.workers.length ?? 0} accent={accent} />
            <Stat label="LLM" value={health?.llmActive ? "ON" : "HEUR"} accent={accent} />
          </div>
          <div className="mt-4 space-y-2">
            <Bar label="CPU" v={vitals.cpu} accent={accent} />
            <Bar label="HEAP" v={vitals.mem} accent={accent} />
          </div>
          <div className="mt-3 font-mono text-[9px] text-[#7fb8d8]">
            UPTIME {Math.floor(uptime / 3600)}h {Math.floor((uptime % 3600) / 60)}m
          </div>
        </div>
      </div>

      {/* RIGHT PANEL — live feed */}
      <div className="absolute right-5 top-1/2 z-20 w-[290px] -translate-y-1/2">
        <div className="glass glass-corner relative p-4">
          <h2 className="mb-3 font-mono text-[10px] tracking-[0.25em]" style={{ color: accent }}>LIVE_FEED</h2>
          <div className="max-h-[46vh] space-y-1.5 overflow-y-auto pr-1">
            {feed.length === 0 && <div className="font-mono text-[10px] text-[#5f8fae]">awaiting activity…</div>}
            {feed.map((it, i) => (
              <div key={i} className="border-l-2 pl-2 font-mono text-[10px] leading-tight" style={{ borderColor: accent }}>
                <span className="opacity-50">[{it.tag}]</span>{" "}
                <span className="text-[#bfe9ff]">{truncate(it.text, 64)}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* BOTTOM CENTER — connections counter (behind voice console, near top of it) */}
      <div className="absolute inset-x-0 top-[8%] z-10 flex flex-col items-center">
        <div className="font-sans text-5xl font-black tracking-tight text-white txt-glow">
          {connections.toLocaleString()}
        </div>
        <div className="font-mono text-[10px] uppercase tracking-[0.3em]" style={{ color: accent }}>neural index · {counts.memories} memories · {counts.skills} skills</div>
        <div className="mt-2 h-1 w-64 overflow-hidden rounded-full bg-white/10">
          <div className="h-full rounded-full" style={{ width: `${Math.min(100, (connections % 200000) / 2000)}%`, background: accent, boxShadow: `0 0 12px ${accent}` }} />
        </div>
      </div>

      {/* VOICE CONSOLE */}
      <VoiceConsole onState={(s) => setThinking(s === "processing" || s === "speaking")} />

      {/* FEED slide-over */}
      <FeedPanel open={feedOpen} onClose={() => setFeedOpen(false)} />
    </div>
  );
}

function Stat({ label, value, accent }: { label: string; value: number | string; accent: string }) {
  return (
    <div>
      <div className="font-mono text-lg font-bold text-white">{value}</div>
      <div className="font-mono text-[8px] tracking-widest" style={{ color: accent, opacity: 0.7 }}>{label}</div>
    </div>
  );
}

function Bar({ label, v, accent }: { label: string; v: number; accent: string }) {
  return (
    <div>
      <div className="flex justify-between font-mono text-[9px] text-[#8fd9ff]">
        <span>{label}</span>
        <span style={{ color: accent }}>{v}%</span>
      </div>
      <div className="mt-0.5 h-1 overflow-hidden rounded-full bg-white/10">
        <div className="h-full rounded-full transition-all duration-700" style={{ width: `${v}%`, background: accent, boxShadow: `0 0 8px ${accent}` }} />
      </div>
    </div>
  );
}

function truncate(s: string, n: number) { return s.length > n ? s.slice(0, n - 1) + "…" : s; }
function abbreviate(n: number) { return n >= 1000 ? (n / 1000).toFixed(0) + "K" : String(n); }
