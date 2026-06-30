import { useState } from "react";
import { api, type BriefResponse } from "../../lib/api";

export default function BriefView() {
  const [brief, setBrief] = useState<BriefResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
    <div>
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
