import { useEffect, useState } from "react";
import { api, type PsychAssessment } from "../../lib/api";

export default function PsychDashboard() {
  const [psych, setPsych] = useState<PsychAssessment | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.tradingPsychology().then(setPsych).catch((e) => setError(String(e)));
  }, []);

  if (error) return <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>;
  if (!psych) return <p className="text-sm text-gray-500">loading psychology…</p>;

  const pct = Math.round(psych.disciplineScore * 100);
  const color = pct >= 80 ? "text-emerald-400" : pct >= 50 ? "text-amber-400" : "text-ultron-danger";

  return (
    <div className="rounded-lg bg-ultron-panel p-4">
      <h3 className="mb-3 text-sm font-semibold text-gray-300">Trading psychology</h3>
      <div className="flex items-end gap-6">
        <div>
          <div className={"text-3xl font-semibold " + color}>{pct}%</div>
          <div className="text-xs uppercase tracking-wide text-gray-500">discipline (30d)</div>
        </div>
        <div className="text-sm text-gray-400">
          <div>trades today: {psych.tradesToday}</div>
          <div>recent losses: {psych.recentLosses}</div>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        {psych.flags.length === 0 ? (
          <span className="rounded bg-emerald-950/40 px-2 py-1 text-xs text-emerald-300">no flags — disciplined</span>
        ) : (
          psych.flags.map((f) => (
            <span key={f} className="rounded bg-red-950/50 px-2 py-1 text-xs text-ultron-danger">
              {f}
            </span>
          ))
        )}
      </div>
    </div>
  );
}
