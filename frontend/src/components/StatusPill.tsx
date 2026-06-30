import { type Health } from "../lib/api";

export default function StatusPill({
  health,
  error,
}: {
  health: Health | null;
  error: string | null;
}) {
  if (error && !health) {
    return <span className="text-sm text-ultron-danger">offline</span>;
  }
  if (!health) {
    return <span className="text-sm text-gray-500">connecting…</span>;
  }
  return (
    <div className="text-right text-xs text-gray-400">
      <div>
        brain: <span className="text-ultron-accent">{health.brain}</span> · embed:{" "}
        <span className="text-ultron-accent">{health.embedder}</span>
      </div>
      <div>workers: {health.workers.length}</div>
      <div>
        auto-approve:{" "}
        <span className={health.autoApprove ? "text-ultron-danger" : "text-gray-300"}>
          {String(health.autoApprove)}
        </span>
      </div>
    </div>
  );
}
