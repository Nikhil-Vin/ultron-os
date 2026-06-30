import { useEffect, useState } from "react";
import { api, type Performance, type TradeDto } from "../../lib/api";

export default function TradeLog({ instrument }: { instrument: string }) {
  const [trades, setTrades] = useState<TradeDto[]>([]);
  const [perf, setPerf] = useState<Performance | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const j = await api.tradingJournal();
      setTrades(j.trades);
      setPerf(j.performance);
    } catch (e) {
      setError(String(e));
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function paper() {
    setBusy(true);
    setError(null);
    try {
      await api.paperTrade({ instrument, side: "BUY", quantity: 50, signalSource: "ui" });
      await load();
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-300">Trade journal (paper)</h3>
        <button
          onClick={paper}
          disabled={busy}
          className="rounded-lg border border-ultron-accent px-3 py-1.5 text-xs font-medium text-ultron-accent disabled:opacity-50"
        >
          {busy ? "Placing…" : `Paper BUY 50 ${instrument}`}
        </button>
      </div>

      {error && <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>}

      {perf && (
        <div className="grid grid-cols-3 gap-2 rounded-lg bg-ultron-panel p-3 text-sm sm:grid-cols-6">
          <Stat label="trades" value={String(perf.totalTrades)} />
          <Stat label="closed" value={String(perf.closedTrades)} />
          <Stat label="wins" value={String(perf.wins)} />
          <Stat label="win rate" value={`${(perf.winRate * 100).toFixed(0)}%`} />
          <Stat label="P&L" value={perf.totalPnl.toFixed(2)} />
          <Stat label="avg R:R" value={perf.avgRiskReward.toFixed(2)} />
        </div>
      )}

      <ul className="space-y-1">
        {trades.map((t) => (
          <li key={t.id} className="flex items-center justify-between rounded-md bg-ultron-panel px-3 py-2 text-sm">
            <span>
              {t.tradeType} {t.quantity} {t.instrument}
              <span className="ml-2 rounded bg-ultron-bg px-1.5 py-0.5 text-xs text-gray-400">{t.executionMode}</span>
            </span>
            <span className="text-xs text-gray-500">
              {t.entryPrice != null ? `@ ${t.entryPrice}` : ""}
            </span>
          </li>
        ))}
        {trades.length === 0 && <li className="text-sm text-gray-500">No trades yet.</li>}
      </ul>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs uppercase tracking-wide text-gray-500">{label}</div>
      <div className="text-gray-200">{value}</div>
    </div>
  );
}
