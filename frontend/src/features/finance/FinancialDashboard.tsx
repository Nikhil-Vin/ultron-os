import { useEffect, useState } from "react";
import { api } from "../../lib/api";

const DEMO_HOLDINGS = [
  { name: "Index funds", value: 850000 },
  { name: "Cash", value: 120000 },
  { name: "Crypto", value: 60000 },
];

export default function FinancialDashboard() {
  const [overview, setOverview] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.financeOverview(DEMO_HOLDINGS, 42000, 60000).then(setOverview).catch((e) => setError(String(e)));
  }, []);

  if (error) return <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>;
  if (!overview) return <p className="text-sm text-gray-500">loading…</p>;

  const money = (n: number) => "₹" + Math.round(n).toLocaleString();

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Card label="Net worth" value={money(overview.netWorth)} accent />
        <Card label="Holdings" value={money(overview.holdingsTotal)} />
        <Card label="Trading P&L" value={money(overview.tradingPnl)} />
        <Card label="Budget left" value={money(overview.budgetRemaining)} />
      </div>

      <section className="rounded-lg bg-ultron-panel p-4">
        <h2 className="mb-3 text-sm font-semibold text-gray-300">Holdings</h2>
        <ul className="space-y-1 text-sm">
          {overview.holdings.map((h: any, i: number) => (
            <li key={i} className="flex justify-between">
              <span className="text-gray-300">{h.name}</span>
              <span className="text-gray-200">{money(h.value)}</span>
            </li>
          ))}
        </ul>
      </section>

      <p className="text-xs text-gray-500">
        Demo holdings shown. Wire a bank/portfolio connector for live aggregation; trading P&L is live from the journal.
      </p>
    </div>
  );
}

function Card({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="rounded-lg bg-ultron-panel p-4">
      <div className="text-xs uppercase tracking-wide text-gray-500">{label}</div>
      <div className={"mt-1 text-xl font-semibold " + (accent ? "text-ultron-accent" : "text-gray-200")}>{value}</div>
    </div>
  );
}
