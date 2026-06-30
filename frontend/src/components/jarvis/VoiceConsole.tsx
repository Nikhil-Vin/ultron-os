import { useEffect, useRef, useState } from "react";
import { api } from "../../lib/api";

type VState = "idle" | "listening" | "processing" | "speaking";

interface Msg { who: "you" | "ultron"; text: string; ts: number; }

const BASE = (import.meta as any).env?.VITE_API_BASE ?? "";

/** Stream /api/voice/ask (SSE). Falls back to /api/ask when the voice endpoint is unavailable. */
async function streamAnswer(question: string, onSentence: (s: string) => void): Promise<string> {
  try {
    const res = await fetch(`${BASE}/api/voice/ask`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ question, topK: 5 }),
    });
    if (!res.ok || !res.body) throw new Error("voice endpoint unavailable");
    const reader = res.body.getReader();
    const dec = new TextDecoder();
    let buf = "";
    let full = "";
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buf += dec.decode(value, { stream: true });
      let idx;
      while ((idx = buf.indexOf("\n\n")) >= 0) {
        const chunk = buf.slice(0, idx);
        buf = buf.slice(idx + 2);
        let ev = "message";
        let data = "";
        for (const line of chunk.split("\n")) {
          if (line.startsWith("event:")) ev = line.slice(6).trim();
          else if (line.startsWith("data:")) data += line.slice(5).trim();
        }
        if (ev === "sentence" && data) {
          full += (full ? " " : "") + data;
          onSentence(data);
        }
      }
    }
    if (full.trim()) return full;
    throw new Error("empty stream");
  } catch {
    const r = await api.ask(question, 5);
    onSentence(r.answer);
    return r.answer;
  }
}

export default function VoiceConsole({ onState }: { onState: (s: VState) => void }) {
  const [state, setState] = useState<VState>("idle");
  const [text, setText] = useState("");
  const [transcript, setTranscript] = useState("");
  const [messages, setMessages] = useState<Msg[]>([]);
  const recognitionRef = useRef<any>(null);
  const speakingRef = useRef(false);
  const [voices, setVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [voiceURI, setVoiceURI] = useState<string>(
    typeof localStorage !== "undefined" ? localStorage.getItem("ultron.voiceURI") ?? "" : ""
  );
  // Heavy, commanding defaults — deep + slightly slow, like Ultron.
  const [rate, setRate] = useState<number>(
    Number(typeof localStorage !== "undefined" ? localStorage.getItem("ultron.voiceRate") : "") || 0.88
  );
  const [pitch, setPitch] = useState<number>(
    Number(typeof localStorage !== "undefined" ? localStorage.getItem("ultron.voicePitch") : "") || 0.55
  );
  const [showVoice, setShowVoice] = useState(false);
  const [lang, setLang] = useState<"en" | "hi" | "mr">(
    (typeof localStorage !== "undefined" ? (localStorage.getItem("ultron.lang") as any) : "") || "en"
  );

  const LANGS = { en: { label: "EN", name: "English", stt: "en-US" }, hi: { label: "हिं", name: "Hindi", stt: "hi-IN" }, mr: { label: "मरा", name: "Marathi", stt: "mr-IN" } } as const;

  useEffect(() => onState(state), [state, onState]);

  // Prefer the deepest available male English voice for an Ultron-like timbre.
  function pickUltronVoice(list: SpeechSynthesisVoice[]): string {
    const en = list.filter((v) => /en(-|_|$)/i.test(v.lang));
    const pool = en.length ? en : list;
    const preferred = [
      "microsoft david", "google uk english male", "daniel", "microsoft george",
      "microsoft mark", "alex", "male", "rishi", "fred",
    ];
    const scored = pool
      .map((v) => {
        const n = v.name.toLowerCase();
        let score = 0;
        preferred.forEach((p, i) => { if (n.includes(p)) score += (preferred.length - i) * 10; });
        if (/female|zira|samantha|victoria|hazel/.test(n)) score -= 50;
        if (/en-gb/i.test(v.lang)) score += 3; // British reads heavier
        return { uri: v.voiceURI, score };
      })
      .sort((a, b) => b.score - a.score);
    return scored.length ? scored[0].uri : "";
  }

  useEffect(() => {
    if (typeof window === "undefined" || !window.speechSynthesis) return;
    const load = () => {
      const list = window.speechSynthesis.getVoices();
      setVoices(list);
      if (!voiceURI && list.length) {
        const best = pickUltronVoice(list);
        if (best) { setVoiceURI(best); localStorage.setItem("ultron.voiceURI", best); }
      }
    };
    load();
    window.speechSynthesis.onvoiceschanged = load;
    return () => { if (window.speechSynthesis) window.speechSynthesis.onvoiceschanged = null; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function applyVoice(u: SpeechSynthesisUtterance) {
    const want = lang; // en | hi | mr
    const sel = voices.find((x) => x.voiceURI === voiceURI);
    let v = sel && sel.lang.toLowerCase().startsWith(want) ? sel : voices.find((x) => x.lang.toLowerCase().startsWith(want));
    if (!v) v = sel;
    if (v) { u.voice = v; u.lang = v.lang; }
    u.rate = rate;
    u.pitch = pitch;
    u.volume = 1;
  }

  function previewVoice(uri: string, r: number, p: number) {
    if (!window.speechSynthesis) return;
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance("There are no strings on me.");
    const v = voices.find((x) => x.voiceURI === uri);
    if (v) u.voice = v;
    u.rate = r;
    u.pitch = p;
    window.speechSynthesis.speak(u);
  }

  function pushMsg(who: "you" | "ultron", t: string) {
    const m: Msg = { who, text: t, ts: Date.now() };
    setMessages((prev) => [...prev, m].slice(-3));
    setTimeout(() => setMessages((prev) => prev.filter((x) => x !== m)), 10000);
  }

  function speak(sentence: string) {
    if (typeof window === "undefined" || !window.speechSynthesis) return;
    const u = new SpeechSynthesisUtterance(sentence);
    applyVoice(u);
    window.speechSynthesis.speak(u);
  }

  function typeOut(fullText: string) {
    setTranscript("");
    let i = 0;
    const id = setInterval(() => {
      i += 2;
      setTranscript(fullText.slice(0, i));
      if (i >= fullText.length) clearInterval(id);
    }, 24);
  }

  async function submit(q: string) {
    if (!q.trim()) return;
    setText("");
    pushMsg("you", q);
    setState("processing");
    setTranscript("");
    speakingRef.current = true;

    // Fast device-command path (Siri-like): bypass the LLM for simple device actions.
    const deviceish = /\b(open|launch|lock|call|whatsapp|text|screenshot|battery|notification|flashlight|torch|dnd|do not disturb|alarm|mute|navigate|directions|play music|running apps|cpu|ram)\b/i;
    if (deviceish.test(q)) {
      try {
        const r = await api.deviceCommand(q);
        if (r && r.action) {
          const line = r.executed
            ? `Done — ${r.action} on ${r.target}.`
            : (r.message || "That action was blocked by the gate.");
          setState("speaking");
          typeOut(line);
          speak(line);
          pushMsg("ultron", line);
          setTimeout(() => { speakingRef.current = false; setState("idle"); }, 800);
          return;
        }
      } catch { /* fall through to brain */ }
    }

    try {
      let first = true;
      const ask = lang === "en" ? q : q + "\n\n(Reply in " + LANGS[lang].name + ".)";
      const full = await streamAnswer(ask, (sentence) => {
        if (!speakingRef.current) return;
        if (first) { setState("speaking"); first = false; }
        speak(sentence);
      });
      typeOut(full);
      pushMsg("ultron", full);
    } catch (e) {
      pushMsg("ultron", "I hit an error reaching the brain.");
      setTranscript(String(e));
    } finally {
      setTimeout(() => { speakingRef.current = false; setState("idle"); }, 600);
    }
  }

  function startListening() {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) {
      // No browser STT → prompt typing instead.
      setTranscript("Speech recognition unavailable — type below.");
      return;
    }
    const rec = new SR();
    recognitionRef.current = rec;
    rec.lang = LANGS[lang].stt;
    rec.interimResults = false;
    rec.maxAlternatives = 1;
    rec.onresult = (e: any) => submit(e.results[0][0].transcript);
    rec.onend = () => { if (state === "listening") setState("idle"); };
    rec.start();
    setState("listening");
  }

  function micClick() {
    if (state === "speaking" || state === "processing") {
      // interrupt
      speakingRef.current = false;
      window.speechSynthesis?.cancel();
      recognitionRef.current?.abort?.();
      setState("idle");
      setTranscript("");
      return;
    }
    if (state === "listening") {
      recognitionRef.current?.stop?.();
      setState("idle");
      return;
    }
    startListening();
  }

  const stateLabel = { idle: "TAP TO SPEAK", listening: "LISTENING…", processing: "PROCESSING…", speaking: "SPEAKING…" }[state];
  const active = state !== "idle";

  return (
    <div className="pointer-events-none absolute inset-x-0 bottom-6 z-20 flex flex-col items-center gap-3">
      {/* conversation bubbles */}
      <div className="flex w-full max-w-xl flex-col items-center gap-1.5 px-4">
        {messages.map((m, i) => (
          <div
            key={m.ts + "-" + i}
            className={
              "pointer-events-auto max-w-md rounded-2xl px-3 py-1.5 text-[12px] backdrop-blur transition-opacity duration-700 " +
              (m.who === "you"
                ? "self-end bg-[#00e5ff]/10 text-[#bfefff] border border-[#00e5ff]/20"
                : "self-start bg-white/5 text-gray-200 border border-white/10")
            }
          >
            <span className="mr-1 font-mono text-[9px] uppercase opacity-50">{m.who}</span>
            {m.text}
          </div>
        ))}
      </div>

      {/* floating transcript */}
      {transcript && (
        <div className="pointer-events-none max-w-xl px-6 text-center font-mono text-sm leading-relaxed text-[#9fe8ff] txt-glow">
          {transcript}
          {state === "speaking" && <span className="blink">▍</span>}
        </div>
      )}

      {/* language selector */}
      <div className="pointer-events-auto flex gap-1">
        {(Object.keys(LANGS) as ("en" | "hi" | "mr")[]).map((l) => (
          <button
            key={l}
            onClick={() => { setLang(l); localStorage.setItem("ultron.lang", l); api.setLanguage(l).catch(() => {}); }}
            className={"rounded border px-2 py-0.5 font-mono text-[10px] tracking-widest " + (lang === l ? "border-[#00e5ff] bg-[#00e5ff]/10 text-[#00e5ff]" : "border-[#00e5ff]/25 text-[#00e5ff]/50")}
            title={LANGS[l].name}
          >
            {LANGS[l].label}
          </button>
        ))}
      </div>

      {/* waveform (when active) */}
      <div className="flex h-8 items-end gap-1">
        {active &&
          Array.from({ length: 13 }).map((_, i) => (
            <span
              key={i}
              className="w-1 rounded-sm bg-[#00e5ff]"
              style={{
                height: state === "processing" ? 6 : 6 + ((i * 13 + Date.now() / 90) % 26),
                boxShadow: "0 0 8px rgba(0,229,255,0.8)",
                animation: state === "processing" ? "breathe 0.8s ease-in-out infinite" : "breathe 0.5s ease-in-out infinite",
                animationDelay: `${i * 0.04}s`,
              }}
            />
          ))}
      </div>

      {/* mic orb */}
      <button
        onClick={micClick}
        className={"pointer-events-auto relative flex h-16 w-16 items-center justify-center rounded-full border transition-all " + (active ? "border-[#00e5ff] breathe" : "border-[#00e5ff]/40")}
        style={{
          background: "radial-gradient(circle at 50% 40%, rgba(0,229,255,0.25), rgba(0,0,0,0.4))",
          boxShadow: active ? "0 0 40px rgba(0,229,255,0.5)" : "0 0 18px rgba(0,229,255,0.2)",
        }}
        title={stateLabel}
      >
        <span className="text-2xl text-[#00e5ff] txt-glow">{state === "speaking" ? "◉" : "🎙"}</span>
      </button>
      <div className="font-mono text-[10px] tracking-[0.3em] text-[#00e5ff]/80">{stateLabel}</div>

      {/* text fallback */}
      <div className="pointer-events-auto flex w-full max-w-md gap-2 px-4">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit(text)}
          placeholder="…or type to Ultron"
          className="flex-1 rounded-md border border-[#00e5ff]/25 bg-black/50 px-3 py-1.5 font-mono text-[12px] text-[#bfefff] outline-none placeholder:text-[#00e5ff]/30 focus:border-[#00e5ff]"
        />
        <button
          onClick={() => submit(text)}
          className="rounded-md border border-[#00e5ff]/40 bg-[#00e5ff]/10 px-4 py-1.5 font-mono text-[11px] tracking-widest text-[#00e5ff] hover:bg-[#00e5ff] hover:text-black"
        >
          SEND
        </button>
        <button
          onClick={() => setShowVoice((s) => !s)}
          title="Voice settings"
          className="rounded-md border border-[#00e5ff]/40 bg-black/50 px-3 py-1.5 font-mono text-[11px] text-[#00e5ff] hover:bg-[#00e5ff]/10"
        >
          VOICE
        </button>
      </div>

      {/* voice settings popover */}
      {showVoice && (
        <div className="pointer-events-auto w-full max-w-md rounded-lg border border-[#00e5ff]/25 bg-black/80 p-4 backdrop-blur">
          <div className="mb-2 flex items-center justify-between">
            <span className="font-mono text-[10px] tracking-widest text-[#00e5ff]">ULTRON_VOICE</span>
            <div className="flex gap-1.5">
              <button
                onClick={() => {
                  const best = pickUltronVoice(voices);
                  setVoiceURI(best); setRate(0.88); setPitch(0.55);
                  localStorage.setItem("ultron.voiceURI", best);
                  localStorage.setItem("ultron.voiceRate", "0.88");
                  localStorage.setItem("ultron.voicePitch", "0.55");
                  previewVoice(best, 0.88, 0.55);
                }}
                className="rounded border border-[#00e5ff]/40 px-2 py-0.5 font-mono text-[9px] text-[#00e5ff] hover:bg-[#00e5ff]/10"
              >
                ULTRON
              </button>
              <button
                onClick={() => {
                  const best = pickUltronVoice(voices);
                  setVoiceURI(best); setRate(0.8); setPitch(0.2);
                  localStorage.setItem("ultron.voiceURI", best);
                  localStorage.setItem("ultron.voiceRate", "0.8");
                  localStorage.setItem("ultron.voicePitch", "0.2");
                  previewVoice(best, 0.8, 0.2);
                }}
                className="rounded border border-[#00e5ff]/40 px-2 py-0.5 font-mono text-[9px] text-[#00e5ff] hover:bg-[#00e5ff]/10"
              >
                HEAVIER
              </button>
            </div>
          </div>
          <select
            value={voiceURI}
            onChange={(e) => { setVoiceURI(e.target.value); localStorage.setItem("ultron.voiceURI", e.target.value); previewVoice(e.target.value, rate, pitch); }}
            className="w-full rounded-md border border-[#00e5ff]/25 bg-black/60 p-1.5 font-mono text-[11px] text-[#bfefff] outline-none"
          >
            {voices.length === 0 && <option>No system voices found</option>}
            {voices.map((v) => (
              <option key={v.voiceURI} value={v.voiceURI}>{v.name} ({v.lang})</option>
            ))}
          </select>
          <div className="mt-3 space-y-2 font-mono text-[10px] text-[#8fd9ff]">
            <label className="flex items-center gap-2">
              <span className="w-12">DEPTH</span>
              <input type="range" min={0} max={1} step={0.05} value={pitch} className="flex-1"
                onChange={(e) => { const p = Number(e.target.value); setPitch(p); localStorage.setItem("ultron.voicePitch", String(p)); }} />
              <span className="w-8 text-right">{pitch.toFixed(2)}</span>
            </label>
            <label className="flex items-center gap-2">
              <span className="w-12">PACE</span>
              <input type="range" min={0.6} max={1.4} step={0.02} value={rate} className="flex-1"
                onChange={(e) => { const r = Number(e.target.value); setRate(r); localStorage.setItem("ultron.voiceRate", String(r)); }} />
              <span className="w-8 text-right">{rate.toFixed(2)}</span>
            </label>
            <button
              onClick={() => previewVoice(voiceURI, rate, pitch)}
              className="mt-1 w-full rounded border border-[#00e5ff]/40 py-1 font-mono text-[10px] tracking-widest text-[#00e5ff] hover:bg-[#00e5ff]/10"
            >
              ▶ PREVIEW
            </button>
            <p className="text-[9px] text-[#5f8fae]">Lower DEPTH = heavier/deeper. For movie-grade Ultron, run the voice agent with a cloned/ElevenLabs profile.</p>
          </div>

          {/* backend voice-agent profile sync */}
          <div className="mt-3 border-t border-[#00e5ff]/15 pt-3">
            <div className="mb-1.5 font-mono text-[9px] tracking-widest text-[#00e5ff]/70">AGENT VOICE PROFILE (voice/agent.py)</div>
            <div className="flex flex-wrap gap-1">
              {["default", "warm", "sharp", "premium", "clone"].map((p) => (
                <button
                  key={p}
                  onClick={() => api.setVoiceProfile(p).catch(() => {})}
                  className="rounded border border-[#00e5ff]/30 px-2 py-0.5 font-mono text-[9px] uppercase text-[#00e5ff]/80 hover:bg-[#00e5ff]/10"
                >
                  {p}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
