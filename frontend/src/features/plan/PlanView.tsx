import { useState } from "react";
import { api, type AgentTrace } from "../../lib/api";

export default function PlanView() {
  const [goal, setGoal] = useState("");
  const [trace, setTrace] = useState<AgentTrace | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function plan() {
    if (!goal.trim()) return;
    setBusy(true);
    setError(null);
    try {
      setTrace(await api.runAgent("plan my day: " + goal));
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        <input
          value={goal}
          onChange={(e) => setGoal(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && plan()}
          placeholder="What do you want to get done? (the Planner will prioritise)"
          className="flex-1 rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
        />
        <button
          onClick={plan}
          disabled={busy || !goal.trim()}
          className="rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50"
        >
          {busy ? "Planning…" : "Plan"}
        </button>
      </div>

      {error && <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>}

      {trace && (
        <div className="rounded-lg bg-ultron-panel p-4">
          <div className="mb-2 text-xs uppercase tracking-wide text-gray-500">
            {trace.worker} · {trace.decision}
          </div>
          <p className="whitespace-pre-wrap text-sm text-gray-200">{trace.result}</p>
        </div>
      )}
    </div>
  );
}
