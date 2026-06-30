import { useState } from "react";
import { api } from "../../lib/api";

const ROUTING_COLOR: Record<string, string> = {
  DELIVER_NOW: "text-emerald-400",
  QUEUE: "text-amber-400",
  SUPPRESS: "text-gray-500",
};

export default function NotificationCenter() {
  const [level, setLevel] = useState("NORMAL");
  const [text, setText] = useState("");
  const [results, setResults] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function route() {
    if (!text.trim()) return;
    setError(null);
    try {
      const r = await api.routeNotification(level, "ui", text);
      setResults((prev) => [r, ...prev].slice(0, 20));
      setText("");
    } catch (e) {
      setError(String(e));
    }
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-400">
        Notifications are routed by your current work mode — focused modes hold non-critical alerts.
      </p>
      <div className="flex gap-2">
        <select value={level} onChange={(e) => setLevel(e.target.value)}
          className="rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200">
          {["LOW", "NORMAL", "HIGH", "CRITICAL"].map((l) => <option key={l}>{l}</option>)}
        </select>
        <input value={text} onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === "Enter" && route()}
          placeholder="notification text…"
          className="flex-1 rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent" />
        <button onClick={route} className="rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg">Route</button>
      </div>

      {error && <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>}

      <ul className="space-y-1">
        {results.map((r, i) => (
          <li key={i} className="flex items-center justify-between rounded-md bg-ultron-panel px-3 py-2 text-sm">
            <span className="text-gray-200">[{r.level}] {r.text}</span>
            <span className={ROUTING_COLOR[r.routing] ?? ""}>{r.routing} · {r.mode}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
