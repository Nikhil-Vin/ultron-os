import { useState } from "react";
import { api, getApiKey, setApiKey } from "../../lib/api";

type Mode = "skill" | "source" | "memory";

/** Slide-over panel to FEED Ultron: text skills, rich sources (URL/file), or memories. */
export default function FeedPanel({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [mode, setMode] = useState<Mode>("skill");
  const [name, setName] = useState("");
  const [content, setContent] = useState("");
  const [url, setUrl] = useState("");
  const [tags, setTags] = useState("");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  function reset() { setName(""); setContent(""); setUrl(""); setTags(""); }

  function loadFile(file: File) {
    if (!name) setName(file.name.replace(/\.[^.]+$/, ""));
    const textExt = /\.(txt|md|markdown|json|csv)$/i.test(file.name);
    if (textExt) {
      const r = new FileReader();
      r.onload = (e) => setContent(String(e.target?.result ?? ""));
      r.readAsText(file);
    } else {
      setMode("source");
      setUrl(`file://${file.name}`);
      setContent(`[File ${file.name} — use a source URL or run the ai-layer to extract.]`);
    }
  }

  async function submit() {
    setBusy(true); setMsg(null);
    try {
      if (mode === "memory") {
        await api.saveMemory({ content, tags: tags || undefined, source: "ui-feed" });
        setMsg("Memory captured.");
      } else if (mode === "source") {
        const s = await api.ingestSkillFromSource({ name, url });
        setMsg(`Ingested "${s.name}".`);
      } else {
        const s = await api.intakeSkill({ name, content, tags: tags || undefined, source: "ui-feed" });
        setMsg(`Learned "${s.name}".`);
      }
      reset();
    } catch (e) {
      setMsg(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div
      className={"pointer-events-none fixed inset-y-0 right-0 z-40 w-[360px] transition-transform duration-300 " + (open ? "translate-x-0" : "translate-x-full")}
    >
      <div className="glass glass-corner pointer-events-auto relative h-full overflow-y-auto p-5">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-mono text-[11px] tracking-[0.3em] text-[#00e5ff] txt-glow">FEED ULTRON</h2>
          <button onClick={onClose} className="font-mono text-[#00e5ff]/70 hover:text-[#00e5ff]">✕</button>
        </div>

        <div className="mb-4 flex gap-1">
          {(["skill", "source", "memory"] as Mode[]).map((m) => (
            <button key={m} onClick={() => setMode(m)}
              className={"flex-1 rounded border px-2 py-1 font-mono text-[10px] uppercase tracking-widest " + (mode === m ? "border-[#00e5ff] bg-[#00e5ff]/10 text-[#00e5ff]" : "border-[#00e5ff]/20 text-[#00e5ff]/50")}>
              {m}
            </button>
          ))}
        </div>

        {mode !== "memory" && (
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Name"
            className="mb-2 w-full rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none focus:border-[#00e5ff]" />
        )}

        {mode === "source" ? (
          <input value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://… (PDF / page / YouTube)"
            className="mb-2 w-full rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none focus:border-[#00e5ff]" />
        ) : (
          <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={6}
            placeholder={mode === "memory" ? "What should Ultron remember?" : "The knowledge / steps to learn…"}
            className="mb-2 w-full rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none focus:border-[#00e5ff]" />
        )}

        <input value={tags} onChange={(e) => setTags(e.target.value)} placeholder="tags (comma-separated)"
          className="mb-2 w-full rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none focus:border-[#00e5ff]" />

        {mode !== "memory" && (
          <label className="mb-3 block cursor-pointer rounded border border-dashed border-[#00e5ff]/30 p-3 text-center font-mono text-[10px] text-[#00e5ff]/60 hover:border-[#00e5ff]/60">
            drop a file or click (txt / md / csv read inline)
            <input type="file" accept=".txt,.md,.csv,.json,.pdf" className="hidden"
              onChange={(e) => e.target.files?.[0] && loadFile(e.target.files[0])} />
          </label>
        )}

        <button onClick={submit} disabled={busy}
          className="w-full rounded border border-[#00e5ff]/50 bg-[#00e5ff]/10 py-2 font-mono text-[11px] tracking-widest text-[#00e5ff] hover:bg-[#00e5ff] hover:text-black disabled:opacity-50">
          {busy ? "FEEDING…" : "FEED →"}
        </button>
        {msg && <p className="mt-3 font-mono text-[11px] text-[#9fe8ff]">{msg}</p>}

        {/* security: API key (only needed when ULTRON_API_KEY gate is enabled) */}
        <div className="mt-5 border-t border-[#00e5ff]/15 pt-3">
          <div className="mb-1 font-mono text-[9px] tracking-widest text-[#00e5ff]/60">SECURITY · X-ULTRON-KEY</div>
          <input
            defaultValue={getApiKey()}
            onBlur={(e) => setApiKey(e.target.value.trim())}
            placeholder="API key (blank = open / local)"
            className="w-full rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[11px] text-[#bfefff] outline-none focus:border-[#00e5ff]"
          />
          <p className="mt-1 text-[9px] text-[#5f8fae]">Set this only if the backend has ULTRON_API_KEY configured.</p>
        </div>
      </div>
    </div>
  );
}
