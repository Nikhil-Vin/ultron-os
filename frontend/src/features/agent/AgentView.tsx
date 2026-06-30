import { useState } from "react";
import { api, type AgentTrace } from "../../lib/api";
import GateModal from "../../components/approval/GateModal";

const RISK_COLOR: Record<string, string> = {
  READ: "text-gray-300",
  LOW: "text-emerald-400",
  HIGH: "text-amber-400",
  CRITICAL: "text-ultron-danger",
};

export default function AgentView() {
  const [instruction, setInstruction] = useState("");
  const [trace, setTrace] = useState<AgentTrace | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [gateOpen, setGateOpen] = useState(false);

  async function run() {
    if (!instruction.trim()) return;
    setBusy(true);
    setError(null);
    try {
      const result = await api.runAgent(instruction);
      setTrace(result);
      setGateOpen(result.decision === "DENIED");
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function approve() {
    if (!trace) return;
    setBusy(true);
    setError(null);
    try {
      const result = await api.approveAgent(trace.instruction);
      setTrace(result);
      setGateOpen(false);
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-4">
      {gateOpen && trace && (
        <GateModal
          trace={trace}
          busy={busy}
          onApprove={approve}
          onCancel={() => setGateOpen(false)}
        />
      )}
      <p className="text-sm text-gray-400">
        Run the full agent loop: perceive → reason → plan → approve → act → remember. HIGH/CRITICAL
        actions are blocked by the approval gate unless explicitly approved.
      </p>

      <div className="flex gap-2">
        <input
          value={instruction}
          onChange={(e) => setInstruction(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && run()}
          placeholder='e.g. "remember the wifi password is hunter2" or "live trade buy AAPL"'
          className="flex-1 rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
        />
        <button
          onClick={run}
          disabled={busy || !instruction.trim()}
          className="rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50"
        >
          {busy ? "Running…" : "Run"}
        </button>
      </div>

      {error && (
        <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>
      )}

      {trace && (
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3 rounded-lg bg-ultron-panel p-4 text-sm sm:grid-cols-4">
            <Field label="worker" value={trace.worker} />
            <Field label="kind" value={trace.kind} />
            <Field
              label="risk"
              value={trace.risk}
              className={RISK_COLOR[trace.risk] ?? "text-gray-300"}
            />
            <Field
              label="decision"
              value={trace.decision}
              className={trace.decision === "DENIED" ? "text-ultron-danger" : "text-emerald-400"}
            />
          </div>

          <div
            className={
              "rounded-lg p-3 text-sm " +
              (trace.acted ? "bg-emerald-950/40 text-emerald-200" : "bg-red-950/40 text-ultron-danger")
            }
          >
            {trace.acted ? "✓ acted" : "✗ not acted"} · audit {trace.auditId.slice(0, 8)}…
          </div>

          <Block title="Result" body={trace.result} />
          <Block title="Reasoning" body={trace.reasoning} />

          <div>
            <h2 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
              Perceived context ({trace.perceived.length})
            </h2>
            <ul className="space-y-2">
              {trace.perceived.map((item) => (
                <li key={`${item.kind}-${item.id}`} className="rounded-lg bg-ultron-panel p-3 text-sm">
                  <span className="text-ultron-accent">
                    {item.kind} · {item.title}
                  </span>
                  <p className="mt-1 text-gray-300">{item.content}</p>
                </li>
              ))}
              {trace.perceived.length === 0 && (
                <li className="text-sm text-gray-500">No prior context retrieved.</li>
              )}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}

function Field({
  label,
  value,
  className = "text-gray-200",
}: {
  label: string;
  value: string;
  className?: string;
}) {
  return (
    <div>
      <div className="text-xs uppercase tracking-wide text-gray-500">{label}</div>
      <div className={className}>{value}</div>
    </div>
  );
}

function Block({ title, body }: { title: string; body: string }) {
  return (
    <div className="rounded-lg bg-ultron-panel p-4">
      <h2 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
        {title}
      </h2>
      <p className="whitespace-pre-wrap text-sm leading-relaxed text-gray-200">{body}</p>
    </div>
  );
}
