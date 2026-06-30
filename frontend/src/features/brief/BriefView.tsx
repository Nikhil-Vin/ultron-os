import { useEffect, useState } from "react";
import { api, type BriefResponse, type Health } from "../../lib/api";

export default function BriefView() {
  const [health, setHealth] = useState<Health | null>(null);
  const [brief, setBrief] = useState<BriefResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.health().then(setHealth).catch((e) => setError(String(e)));
  }, []);

  async function generate() {
    setLoading(true);
    setError(null);
    try {
      setBrief(await api.brief());
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl p-6">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-ultron-accent">Ultron-OS</h1>
          <p className="text-sm text-gray-400">Morning Brief · Phase 0</p>
        </div>
        <StatusPill health={health} error={error} />
      </header>

      <button
        onClick={generate}
        disabled={loading}
        className="rounded-lg bg-ultron-accent px-4 py-2 font-medium text-ultron-bg disabled:opacity-50"
      >
        {loading ? "Thinking…" : "Generate brief"}
      </button>

      {error && (
        <p className="mt-4 rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">
          {error} — is the backend running on :8080?
        </p>
      )}

      {brief && (
        <pre className="mt-6 whitespace-pre-wrap rounded-lg bg-ultron-panel p-4 text-sm leading-relaxed text-gray-200">
          {brief.brief}
        </pre>
      )}
    </div>
  );
}

function StatusPill({
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
        brain: <span className="text-ultron-accent">{health.brain}</span>
      </div>
      <div>github: {health.github}</div>
      <div>auto-approve: {String(health.autoApprove)}</div>
    </div>
  );
}
