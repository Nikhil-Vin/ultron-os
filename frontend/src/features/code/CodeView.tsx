import { useState } from "react";
import { api } from "../../lib/api";

export default function CodeView() {
  const [prompt, setPrompt] = useState("");
  const [name, setName] = useState("project");
  const [lang, setLang] = useState("Node.js");
  const [busy, setBusy] = useState(false);
  const [res, setRes] = useState<{ projectDir: string; files: string[]; notes: string | null } | null>(null);
  const [err, setErr] = useState<string | null>(null);

  async function gen() {
    if (!prompt.trim()) return;
    setBusy(true); setErr(null);
    try { setRes(await api.codeGenerate(prompt, name, lang)); }
    catch (e) { setErr(String(e)); } finally { setBusy(false); }
  }

  return (
    <div className="space-y-3">
      <h2 className="font-mono text-[11px] tracking-[0.25em] text-[#00e5ff]">CODE GENERATOR</h2>
      <textarea value={prompt} onChange={(e) => setPrompt(e.target.value)} rows={3}
        placeholder="e.g. build a REST API for a todo app"
        className="w-full rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none focus:border-[#00e5ff]" />
      <div className="flex gap-2">
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="project name"
          className="flex-1 rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none" />
        <input value={lang} onChange={(e) => setLang(e.target.value)} placeholder="language"
          className="w-32 rounded border border-[#00e5ff]/25 bg-black/50 p-2 font-mono text-[12px] text-[#bfefff] outline-none" />
        <button onClick={gen} disabled={busy}
          className="rounded border border-[#00e5ff]/50 bg-[#00e5ff]/10 px-4 py-2 font-mono text-[11px] tracking-widest text-[#00e5ff] hover:bg-[#00e5ff] hover:text-black disabled:opacity-50">
          {busy ? "GENERATING…" : "GENERATE →"}
        </button>
      </div>
      {err && <p className="rounded bg-red-950/50 p-2 text-[12px] text-[#f87171]">{err}</p>}
      {res && (
        <div className="rounded border border-[#00e5ff]/20 bg-black/40 p-3 font-mono text-[11px] text-[#9fe8ff]">
          <div className="mb-1 text-[#00e5ff]">{res.projectDir}</div>
          {res.files.map((f, i) => <div key={i}>· {f}</div>)}
          {res.notes && <pre className="mt-2 whitespace-pre-wrap text-gray-300">{res.notes}</pre>}
        </div>
      )}
    </div>
  );
}
