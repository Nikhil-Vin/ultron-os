import { useEffect, useState } from "react";
import { api, type Health, type MemoryDto, type SkillDto, type TradingSignalDto } from "../lib/api";
import NeuralSphere from "./jarvis/NeuralSphere";
import VoiceConsole from "./jarvis/VoiceConsole";
import FeedPanel from "./jarvis/FeedPanel";
import BriefView from "../features/brief/BriefView";
import AgentView from "../features/agent/AgentView";
import SkillsView from "../features/skills/SkillsView";
import TradingDashboard from "../features/trading/TradingDashboard";
import MemoryView from "../features/memory/MemoryView";
import DevicesView from "../features/devices/DevicesView";
import CodeView from "../features/code/CodeView";

const TABS = ["core", "brief", "agent", "skills", "trading", "memory", "devices", "code"] as const;
type Tab = (typeof TABS)[number];

export default function CommandCenter() {
  const [tab, setTab] = useState<Tab>("core");
  const [mode, setMode] = useState<"default" | "trading" | "research">("default");
  const [thinking, setThinking] = useState(false);
  const [feedOpen, setFeedOpen] = useState(false);
  const [clock, setClock] = useState("");
  const [bootLog, setBootLog] = useState<string[]>(["BOOTING ULTRON CORE v4.2 ..."]);
  const [bootComplete, setBootComplete] = useState(false);

  const [health, setHealth] = useState<Health | null>(null);
  const [sys, setSys] = useState<any>(null);
  const [memories, setMemories] = useState<MemoryDto[]>([]);
  const [skills, setSkills] = useState<SkillDto[]>([]);
  const [signals, setSignals] = useState<TradingSignalDto[]>([]);
  const [devices, setDevices] = useState<{ type: string; online: boolean }[]>([]);
  const [providers, setProviders] = useState<{ name: string; available: boolean }[]>([]);
  const [pinned, setPinned] = useState("auto");

  useEffect(() => {
    setMode(tab === "trading" ? "trading" : tab === "agent" ? "research" : "default");
  }, [tab]);

  useEffect(() => {
    async function bootSequence() {
      const services = [
        { name: "BACKEND (Spring Boot)", check: async () => {
          try {
            await api.health();
            return true;
          } catch {
            return false;
          }
        }},
        { name: "MEMORY MATRIX (pgvector)", check: async () => {
          try {
            await api.systemStats();
            return true;
          } catch {
            return false;
          }
        }},
        { name: "BRAIN (LLM)", check: async () => {
          try {
            const h = await api.health();
            return h.llmActive === true;
          } catch {
            return false;
          }
        }},
        { name: "EMBEDDER (vector engine)", check: async () => {
          try {
            const h = await api.health();
            return h.embedder === "ollama" || h.embedder === "heuristic";
          } catch {
            return false;
          }
        }},
        { name: "GOVERNANCE GATE (human-in-the-loop)", check: async () => true }, // Always ready
      ];

      for (const svc of services) {
        const status = await svc.check();
        const msg = `${svc.name} ... ${status ? "✓ ONLINE" : "✗ OFFLINE"}`;
        setBootLog(prev => [...prev, msg]);
        await new Promise(r => setTimeout(r, 300));
      }
      
      setBootLog(prev => [...prev, "ULTRON CORE READY"]);
      setBootComplete(true);
    }

    bootSequence();

    const c = setInterval(() => {
      const n = new Date();
      setClock([n.getHours(), n.getMinutes(), n.getSeconds()].map((x) => String(x).padStart(2, "0")).join(":"));
    }, 1000);
    return () => { clearInterval(c); };
  }, []);

  useEffect(() => {
    let alive = true;
    
    // Initial load
    async function loadAll() {
      const [h, sysData, mem, sk, sig, dv, bp] = await Promise.all([
        api.health().catch(() => null),
        api.systemStats().catch(() => null),
        api.recall("").catch(() => []),
        api.listSkills().catch(() => []),
        api.tradingSignals().catch(() => []),
        api.devices().catch(() => []),
        api.brainProviders().catch(() => null),
      ]);
      if (!alive) return;
      setHealth(h); setSys(sysData); setMemories(mem); setSkills(sk);
      setSignals(sig); setDevices(dv);
      if (bp) { setProviders(bp.providers); setPinned(bp.pinned); }
    }
    
    loadAll();
    
    // Separate polling intervals per requirement:
    // - System stats every 5s (CPU/memory/uptime)
    // - Devices every 10s (connection status)
    // - Health every 30s (providers/workers)
    // - Data (memories/skills/signals) every 15s
    
    const systemInterval = setInterval(() => {
      if (!alive) return;
      api.systemStats().then(setSys).catch(() => {});
    }, 5000);
    
    const devicesInterval = setInterval(() => {
      if (!alive) return;
      api.devices().then(setDevices).catch(() => {});
    }, 10000);
    
    const healthInterval = setInterval(() => {
      if (!alive) return;
      Promise.all([
        api.health().then(setHealth).catch(() => {}),
        api.brainProviders().then(bp => {
          if (bp) { setProviders(bp.providers); setPinned(bp.pinned); }
        }).catch(() => {}),
      ]);
    }, 30000);
    
    const dataInterval = setInterval(() => {
      if (!alive) return;
      Promise.all([
        api.recall("").then(setMemories).catch(() => {}),
        api.listSkills().then(setSkills).catch(() => {}),
        api.tradingSignals().then(setSignals).catch(() => {}),
      ]);
    }, 15000);
    
    return () => { 
      alive = false; 
      clearInterval(systemInterval);
      clearInterval(devicesInterval);
      clearInterval(healthInterval);
      clearInterval(dataInterval);
    };
  }, []);

  // Force reload data when agent creates memory
  function refreshData() {
    Promise.all([
      api.systemStats().then(setSys).catch(() => {}),
      api.recall("").then(setMemories).catch(() => {}),
    ]);
  }

  const accent = mode === "trading" ? "#ffb300" : mode === "research" ? "#9b5cff" : "#00e5ff";
  const devOnline = devices.filter((d) => d.online).length;
  const sigColor: Record<string, string> = { BUY: "#34d399", SELL: "#f87171", HOLD: "#94a3b8" };

  return (
    <div className="relative h-screen w-screen overflow-hidden bg-black text-[#cfe9ff]">
      {tab === "core" && <NeuralSphere mode={mode} thinking={thinking} memoryCount={sys?.memories ?? memories.length} />}

      {/* ===== HEADER ===== */}
      <header className="absolute inset-x-0 top-0 z-40 flex items-center justify-between gap-3 border-b border-[#00e5ff]/25 bg-black/90 px-4 py-2 backdrop-blur">
        <div className="flex items-center gap-3">
          <span className="text-xl txt-glow" style={{ color: accent }}>◆</span>
          <div>
            <div className="font-mono text-base font-black tracking-[0.2em] txt-glow" style={{ color: accent }}>ULTRON OS</div>
            <div className="font-mono text-[9px] tracking-[0.3em] text-[#5f8fae]">v4.2 · JARVIS CORE</div>
          </div>
        </div>

        {/* tab nav */}
        <nav className="flex flex-wrap gap-1">
          {TABS.map((t) => (
            <button key={t} onClick={() => setTab(t)}
              className={"px-2.5 py-1 font-mono text-[10px] uppercase tracking-widest transition-all " +
                (tab === t ? "border-b-2 text-[#00e5ff] txt-glow" : "border-b-2 border-transparent text-[#00e5ff]/45 hover:text-[#00e5ff]/80")}
              style={tab === t ? { borderColor: accent, color: accent } : {}}>
              {t}
            </button>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          <Pill label="BRAIN" value={(health?.brain ?? "—").toUpperCase() + (health?.llmActive ? "·LLM" : "")} color={accent} />
          <Pill label="DEVICES" value={String(devOnline)} color={devOnline ? "#34d399" : "#5f8fae"} />
          <Pill label="GATE" value={health?.autoApprove ? "AUTO" : "SECURE"} color={health?.autoApprove ? "#f87171" : "#34d399"} />
          <select value={pinned}
            onChange={async (e) => { const p = e.target.value; setPinned(p); await api.selectBrain(p).catch(() => {}); }}
            className="bg-black/70 px-1.5 py-1 font-mono text-[10px] outline-none" style={{ color: accent, border: `1px solid ${accent}33` }}>
            <option value="auto">AUTO</option>
            {providers.map((p) => <option key={p.name} value={p.name} disabled={!p.available && p.name !== "heuristic" && p.name !== "ollama"}>{p.name.toUpperCase()}</option>)}
          </select>
          <span className="font-mono text-[12px] font-bold txt-glow" style={{ color: accent }}>{clock}</span>
          <button onClick={() => setFeedOpen(true)} className="border px-2 py-1 font-mono text-[10px] tracking-widest" style={{ color: accent, borderColor: `${accent}55` }}>＋FEED</button>
          <button onClick={() => setFeedOpen((s) => !s)} className="border px-2 py-1 font-mono text-[10px]" style={{ color: accent, borderColor: `${accent}55` }}>⚙</button>
        </div>
      </header>

      {/* ===== LEFT PANEL ===== */}
      <aside className={"absolute left-3 top-16 z-20 flex w-[230px] flex-col gap-3 " + (tab === "core" ? "" : "hidden")}>
        <Panel title="SYSTEM VITALS" accent={accent}>
          <Bar label="CPU" v={sys?.cpuPercent ?? 0} accent={accent} />
          <Bar label="HEAP" v={sys?.heapPercent ?? 0} accent={accent} />
          <div className="mt-2 grid grid-cols-2 gap-2 font-mono text-[10px] text-[#8fd9ff]">
            <Stat l="MEMORIES" v={sys?.memories ?? memories.length} />
            <Stat l="SKILLS" v={sys?.skills ?? skills.length} />
            <Stat l="CORES" v={sys?.processors ?? "—"} />
            <Stat l="UPTIME" v={sys ? Math.floor(sys.uptimeSeconds / 60) + "m" : "—"} />
          </div>
        </Panel>
        <Panel title="RECENT MEMORY" accent={accent}>
          <div className="max-h-[26vh] space-y-1 overflow-y-auto">
            {memories.length === 0 && <div className="font-mono text-[10px] text-[#5f8fae]">no memories yet</div>}
            {memories.slice(0, 8).map((m) => (
              <div key={m.id} className="border-l-2 pl-2 font-mono text-[10px] leading-tight text-[#bfe9ff]" style={{ borderColor: accent }}>
                <span className="opacity-50">[{m.type}]</span> {m.content.slice(0, 70)}
              </div>
            ))}
          </div>
        </Panel>
      </aside>

      {/* ===== RIGHT PANEL ===== */}
      <aside className={"absolute right-3 top-16 z-20 flex w-[260px] flex-col gap-3 " + (tab === "core" ? "" : "hidden")}>
        <Panel title="TRADING SIGNALS" accent={accent}>
          <div className="max-h-[24vh] space-y-1 overflow-y-auto">
            {signals.length === 0 && <div className="font-mono text-[10px] text-[#5f8fae]">no signals yet</div>}
            {signals.slice(0, 8).map((s) => (
              <div key={s.id} className="flex items-center justify-between font-mono text-[10px]">
                <span><span style={{ color: sigColor[s.signalType] }}>{s.signalType}</span> {s.instrument}</span>
                <span className="text-[#5f8fae]">{(s.confidence * 100).toFixed(0)}%</span>
              </div>
            ))}
          </div>
        </Panel>
        <Panel title="DEVICES" accent={accent}>
          {devices.length === 0 && <div className="font-mono text-[10px] text-[#5f8fae]">none — run an agent</div>}
          {devices.map((d, i) => (
            <div key={i} className="flex justify-between font-mono text-[10px]">
              <span>{d.type === "android" ? "📱 phone" : "💻 " + d.type}</span>
              <span style={{ color: d.online ? "#34d399" : "#5f8fae" }}>{d.online ? "ONLINE" : "OFF"}</span>
            </div>
          ))}
        </Panel>
        <Panel title="BOOT LOG" accent={accent}>
          <div className="font-mono text-[9px] leading-relaxed text-[#8fd9ff]">
            {bootLog.map((l, i) => <div key={i}><span className="opacity-40">›</span> {l}</div>)}
            {bootComplete && <div className="blink" style={{ color: accent }}>█ READY</div>}
          </div>
        </Panel>
      </aside>

      {/* ===== FEATURE VIEW as primary content (non-core tabs) ===== */}
      {tab !== "core" && (
        <main className="absolute inset-x-0 bottom-0 top-[52px] z-20 overflow-y-auto bg-black/80 px-6 py-6 backdrop-blur">
          <div className="mx-auto max-w-4xl">
            <button onClick={() => setTab("core")} className="mb-4 font-mono text-[11px] tracking-widest text-[#00e5ff]/70 hover:text-[#00e5ff]">← BACK TO CORE</button>
            {tab === "brief" && <BriefView />}
            {tab === "agent" && <AgentView onMemoryChange={refreshData} />}
            {tab === "skills" && <SkillsView />}
            {tab === "trading" && <TradingDashboard />}
            {tab === "memory" && <MemoryView />}
            {tab === "devices" && <DevicesView />}
            {tab === "code" && <CodeView />}
          </div>
        </main>
      )}

      {/* ===== VOICE CONSOLE (Core tab only) ===== */}
      {tab === "core" && <VoiceConsole onState={(s) => setThinking(s === "processing" || s === "speaking")} />}

      {/* ===== FEED SLIDE-OVER ===== */}
      <FeedPanel open={feedOpen} onClose={() => setFeedOpen(false)} />
    </div>
  );
}

function Pill({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <div className="hidden flex-col items-end leading-tight md:flex">
      <span className="font-mono text-[7px] tracking-widest text-[#5f8fae]">{label}</span>
      <span className="font-mono text-[10px] font-bold" style={{ color }}>{value}</span>
    </div>
  );
}

function Panel({ title, accent, children }: { title: string; accent: string; children: React.ReactNode }) {
  return (
    <section className="glass glass-corner relative p-3" style={{ background: "rgba(0,0,0,0.7)" }}>
      <h2 className="mb-2 font-mono text-[10px] tracking-[0.25em]" style={{ color: accent }}>{title}</h2>
      {children}
    </section>
  );
}

function Bar({ label, v, accent }: { label: string; v: number; accent: string }) {
  return (
    <div className="mb-1.5">
      <div className="flex justify-between font-mono text-[9px] text-[#8fd9ff]"><span>{label}</span><span style={{ color: accent }}>{v}%</span></div>
      <div className="mt-0.5 h-1 overflow-hidden rounded-full bg-white/10">
        <div className="h-full rounded-full transition-all duration-700" style={{ width: `${v}%`, background: accent, boxShadow: `0 0 8px ${accent}` }} />
      </div>
    </div>
  );
}

function Stat({ l, v }: { l: string; v: number | string }) {
  return <div><div className="text-sm font-bold text-white">{v}</div><div className="text-[8px] tracking-widest text-[#5f8fae]">{l}</div></div>;
}
