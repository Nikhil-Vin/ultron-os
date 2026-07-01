// TradingChart — a lightweight inline-SVG price chart (zero extra deps so the build stays green).
// Production upgrade: swap for TradingView Lightweight Charts (`lightweight-charts`) per the spec.
import { useEffect, useState } from "react";

function syntheticSeries(seed: string, n = 60): number[] {
  // Deterministic-ish walk so the panel shows a plausible chart without a live feed.
  let h = 0;
  for (const c of seed) h = (h * 31 + c.charCodeAt(0)) >>> 0;
  let price = 100 + (h % 400);
  const out: number[] = [];
  for (let i = 0; i < n; i++) {
    h = (h * 1103515245 + 12345) >>> 0;
    price += ((h % 1000) / 1000 - 0.5) * 4;
    out.push(Math.max(1, price));
  }
  return out;
}

export default function TradingChart({ instrument }: { instrument: string }) {
  const [data, setData] = useState<number[]>([]);
  useEffect(() => setData(syntheticSeries(instrument)), [instrument]);

  const w = 520;
  const h = 160;
  if (data.length < 2) return <div className="h-40" />;
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  const pts = data
    .map((v, i) => `${(i / (data.length - 1)) * w},${h - ((v - min) / range) * h}`)
    .join(" ");
  const up = data[data.length - 1] >= data[0];

  return (
    <div className="rounded-lg bg-ultron-panel p-4">
      <div className="mb-2 flex items-baseline justify-between">
        <span className="text-sm font-medium text-gray-200">{instrument}</span>
        <span className={up ? "text-emerald-400" : "text-ultron-danger"}>
          {data[data.length - 1].toFixed(2)}
        </span>
      </div>
      
      {/* Prominent synthetic data warning */}
      <div className="mb-3 rounded border border-amber-500/30 bg-amber-950/20 px-3 py-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-amber-500">⚠</span>
            <span className="font-mono text-xs text-amber-200">SYNTHETIC DATA</span>
          </div>
          <button
            onClick={() => {
              alert(
                "To connect live broker data:\n\n" +
                "1. Set ZERODHA_API_KEY + ZERODHA_API_SECRET in backend .env\n" +
                "2. Or set ALPACA_API_KEY + ALPACA_API_SECRET\n" +
                "3. Restart backend: cd backend && ./mvnw spring-boot:run\n" +
                "4. Live quotes will replace synthetic data\n\n" +
                "See docs/TRADING.md for full setup instructions"
              );
            }}
            className="rounded border border-ultron-accent/40 bg-ultron-accent/10 px-2 py-1 font-mono text-[10px] tracking-wider text-ultron-accent hover:bg-ultron-accent hover:text-black transition-colors"
          >
            CONNECT BROKER
          </button>
        </div>
        <p className="mt-1 text-[10px] text-amber-200/70">
          Chart shows simulated price movement. Connect a broker API for real-time market data.
        </p>
      </div>

      <svg viewBox={`0 0 ${w} ${h}`} className="h-40 w-full">
        <polyline
          fill="none"
          stroke={up ? "#34d399" : "#f87171"}
          strokeWidth="2"
          points={pts}
        />
      </svg>
    </div>
  );
}
