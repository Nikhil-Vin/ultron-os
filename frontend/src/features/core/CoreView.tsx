import { useEffect, useState } from "react";
import { api, type Health } from "../../lib/api";
import UltronOrb from "../../components/three/UltronOrb";

/**
 * Core — the GOLD CORE HUD centerpiece. The particle nucleus sits behind a glass voice panel that
 * reflects live brain/embedder status and a waveform. Talk to Ultron via /api/voice/command.
 */
export default function CoreView() {
  const [health, setHealth] = useState<Health | null>(null);
  const [instruction, setInstruction] = useState("");
  const [spoken, setSpoken] = useState<string | null>(null);
  const [voiceState, setVoiceState] = useState("idle");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.health().then(setHealth).catch(() => {});
  }, []);

  async function command() {
    if (!instruction.trim()) return;
    setBusy(true);
    setVoiceState("thinking");
    setSpoken(null);
    try {
      const res = await fetch("/api/voice/command", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ instruction }),
      }).then((r) => r.json());
      setSpoken(res.spoken ?? JSON.stringify(res));
      setVoiceState("speaking");
      setTimeout(() => setVoiceState("idle"), 2500);
    } catch (e) {
      setSpoken(String(e));
      setVoiceState("idle");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="relative -m-6 h-[calc(100vh-160px)] overflow-hidden rounded-lg border border-ultron-gold/20 bg-ultron-obsidian">
      {/* hex grid backdrop */}
      <div
        className="absolute inset-0"
        style={{
          backgroundImage: "radial-gradient(circle at 2px 2px, rgba(212,175,55,0.06) 1.5px, transparent 0)",
          backgroundSize: "32px 32px",
        }}
      />
      {/* orb */}
      <div className="absolute inset-0">
        <UltronOrb voiceState={voiceState} />
      </div>

      {/* glass voice panel */}
      <div className="relative z-10 flex h-full flex-col items-center justify-center px-6">
        <div
          className="w-full max-w-xl p-8 text-center"
          style={{
            background: "rgba(13,13,13,0.55)",
            backdropFilter: "blur(16px)",
            border: "1px solid rgba(212,175,55,0.4)",
            boxShadow: "0 0 50px rgba(212,175,55,0.15)",
          }}
        >
          <p className="mb-6 font-mono text-[11px] uppercase tracking-[0.5em] text-ultron-gold">
            NEURAL_ASSISTANT · GOLD_CORE
          </p>
          <div className="mb-4 flex h-12 items-end justify-center gap-1.5">
            {Array.from({ length: 9 }).map((_, i) => (
              <div
                key={i}
                className="w-[3px] rounded-sm bg-ultron-gold"
                style={{
                  height: voiceState === "idle" ? 6 : 8 + ((i * 17) % 40),
                  boxShadow: "0 0 10px rgba(212,175,55,0.8)",
                  transition: "height 0.15s",
                }}
              />
            ))}
          </div>
          <h1
            className="text-5xl font-black uppercase tracking-tighter text-white"
            style={{ textShadow: "0 0 20px rgba(255,255,255,0.4)" }}
          >
            ULTRON
          </h1>
          <p className="mt-2 font-mono text-[11px] uppercase tracking-widest text-ultron-gold/80">
            {health
              ? `${health.brain}${health.llmActive ? " · LLM ACTIVE" : ""} · ${health.workers.length} workers · ready`
              : "connecting…"}
          </p>

          <div className="mt-6 flex gap-2">
            <input
              value={instruction}
              onChange={(e) => setInstruction(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && command()}
              placeholder="Speak to Ultron…"
              className="flex-1 border border-ultron-gold/40 bg-black/50 px-3 py-2 font-mono text-sm text-ultron-gold outline-none placeholder:text-ultron-gold/30 focus:border-ultron-gold"
            />
            <button
              onClick={command}
              disabled={busy}
              className="border border-ultron-gold/60 bg-ultron-gold/10 px-5 py-2 font-bold uppercase tracking-widest text-ultron-gold transition-colors hover:bg-ultron-gold hover:text-ultron-obsidian disabled:opacity-50"
            >
              {busy ? "···" : "Send"}
            </button>
          </div>

          {spoken && (
            <p className="mt-5 whitespace-pre-wrap text-left text-sm leading-relaxed text-gray-200">{spoken}</p>
          )}
        </div>
      </div>
    </div>
  );
}
