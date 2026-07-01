import { useState } from "react";
import TradingChart from "./TradingChart";
import SignalFeed from "./SignalFeed";
import TradeLog from "./TradeLog";
import PsychDashboard from "./PsychDashboard";
import PreTradeChecklist from "./PreTradeChecklist";

const WATCHLIST = ["NIFTY50", "RELIANCE", "BTCUSD", "AAPL"];

export default function TradingDashboard() {
  const [instrument, setInstrument] = useState(WATCHLIST[0]);

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-2">
        {WATCHLIST.map((w) => (
          <button
            key={w}
            onClick={() => setInstrument(w)}
            className={
              "rounded-lg px-3 py-1.5 text-sm " +
              (instrument === w
                ? "bg-ultron-accent text-ultron-bg"
                : "border border-gray-700 text-gray-300 hover:border-ultron-accent")
            }
          >
            {w}
          </button>
        ))}
        <span className="ml-auto rounded bg-ultron-panel px-2 py-1 text-xs text-gray-400">
          paper-trading default · live execution gated
        </span>
      </div>

      <TradingChart instrument={instrument} />

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        <SignalFeed instrument={instrument} />
        <PreTradeChecklist instrument={instrument} />
        <PsychDashboard />
      </div>

      <TradeLog instrument={instrument} />
    </div>
  );
}
