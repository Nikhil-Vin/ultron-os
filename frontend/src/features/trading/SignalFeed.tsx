import { useEffect, useState } from "react";
import { api, type TradingAdvice, type TradingSignalDto } from "../../lib/api";

const COLOR: Record<string, string> = {
  BUY: "text-emerald-400",
  SELL: "text-ultron-danger",
  HOLD: "text-gray-300",
};

export default function SignalFeed({ instrument }: { instrument: string }) {
  const [signals, setSignals] = useState<TradingSignalDto[]>([]);
  const [advice, setAdvice] = useState<TradingAdvice | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      setSignals(await api.tradingSignals());
    } catch (e) {
      setError(String(e));
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function generate() {
    setBusy(true);
    setError(null);
    try {
      // Demo indicator snapshot; a live feed / ai-layer fills these in production.
      const a = await api.tradingSignal(instrument, {
        rsi: 28, macd: 1.2, macdSignal: 0.8, sentiment: 0.3,
      });
      setAdvice(a);
      await load();
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-3">
      <button
        onClick={generate}
        disabled={busy}
        className="rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50"
      >
        {busy ? "Analyzing…" : `Generate signal for ${instrument}`}
      </button>

      {error && <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>}

      {advice && (
        <div className="rounded-lg bg-ultron-panel p-4">
          <div className={"text-lg font-semibold " + (COLOR[advice.signal.signalType] ?? "")}>
            {advice.signal.signalType} · {(advice.signal.confidence * 100).toFixed(0)}%
          </div>
          <p className="mt-1 text-sm text-gray-300">{advice.signal.reasoning}</p>
          <p className="mt-2 whitespace-pre-wrap text-sm text-gray-200">{advice.narrative}</p>
          <p className="mt-1 text-xs text-gray-500">
            rules applied: {advice.rulesApplied} · knowledge: {advice.knowledgeUsed}
          </p>
        </div>
      )}

      <ul className="space-y-1">
        {signals.map((s) => (
          <li key={s.id} className="flex items-center justify-between rounded-md bg-ultron-panel px-3 py-2 text-sm">
            <span>
              <span className={COLOR[s.signalType] ?? ""}>{s.signalType}</span> · {s.instrument}
            </span>
            <span className="text-xs text-gray-500">{new Date(s.createdAt).toLocaleTimeString()}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
