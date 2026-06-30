import { type AgentTrace } from "../../lib/api";

export default function GateModal({
  trace,
  busy,
  onApprove,
  onCancel,
}: {
  trace: AgentTrace;
  busy: boolean;
  onApprove: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-md rounded-xl border border-ultron-danger/40 bg-ultron-panel p-5 shadow-xl">
        <h2 className="text-lg font-semibold text-ultron-danger">Approval required</h2>
        <p className="mt-2 text-sm text-gray-300">
          The agent planned a{" "}
          <span className="font-semibold text-ultron-danger">{trace.risk}</span> action that the
          approval gate <span className="font-semibold">blocked</span>. Review before authorising.
        </p>

        <dl className="mt-4 space-y-1 rounded-lg bg-ultron-bg p-3 text-sm">
          <Row label="instruction" value={trace.instruction} />
          <Row label="worker" value={`${trace.worker} · ${trace.kind}`} />
          <Row label="risk" value={trace.risk} danger />
          <Row label="audit id" value={trace.auditId} />
        </dl>

        <p className="mt-3 text-xs text-gray-500">
          Approving records a second, explicit audit entry. This is deliberate human-in-the-loop
          authorisation — not auto-approval.
        </p>

        <div className="mt-5 flex justify-end gap-2">
          <button
            onClick={onCancel}
            disabled={busy}
            className="rounded-lg border border-gray-700 px-4 py-2 text-sm text-gray-200 hover:border-gray-500 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={onApprove}
            disabled={busy}
            className="rounded-lg bg-ultron-danger px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50"
          >
            {busy ? "Authorising…" : "Approve & run"}
          </button>
        </div>
      </div>
    </div>
  );
}

function Row({
  label,
  value,
  danger = false,
}: {
  label: string;
  value: string;
  danger?: boolean;
}) {
  return (
    <div className="flex justify-between gap-3">
      <dt className="text-gray-500">{label}</dt>
      <dd className={"truncate text-right " + (danger ? "text-ultron-danger" : "text-gray-200")}>
        {value}
      </dd>
    </div>
  );
}
