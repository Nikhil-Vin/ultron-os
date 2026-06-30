import { useEffect, useState } from "react";
import { api, type SkillDto } from "../../lib/api";
import SkillIntakeForm from "./SkillIntakeForm";

export default function SkillsView() {
  const [query, setQuery] = useState("");
  const [skills, setSkills] = useState<SkillDto[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function load(q = query) {
    setError(null);
    try {
      setSkills(await api.listSkills(q));
    } catch (e) {
      setError(String(e));
    }
  }

  useEffect(() => {
    load("");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="space-y-6">
      <SkillIntakeForm onLearned={() => load("")} />

      <section>
        <div className="mb-3 flex gap-2">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && load()}
            placeholder="Search the skill library…"
            className="flex-1 rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
          />
          <button
            onClick={() => load()}
            className="rounded-lg border border-gray-700 px-4 py-2 text-sm text-gray-200 hover:border-ultron-accent"
          >
            Search
          </button>
        </div>

        {error && (
          <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>
        )}

        <ul className="space-y-2">
          {skills.map((s) => (
            <li key={s.id} className="rounded-lg bg-ultron-panel p-3 text-sm">
              <div className="flex items-center justify-between">
                <p className="font-medium text-ultron-accent">{s.name}</p>
                {s.status === "paused" && (
                  <span className="rounded bg-yellow-900/50 px-2 py-0.5 text-xs text-yellow-400">
                    paused
                  </span>
                )}
              </div>
              {s.description && <p className="text-gray-300">{s.description}</p>}
              <p className="mt-1 whitespace-pre-wrap text-gray-200">{s.content}</p>
              <div className="mt-1 flex items-center justify-between">
                <p className="text-xs text-gray-500">{s.tags ?? "no tags"}</p>
                <div className="flex gap-1">
                  <button
                    onClick={async () => {
                      try {
                        if (s.status === "paused") {
                          await api.resumeSkill(s.id);
                        } else {
                          await api.pauseSkill(s.id);
                        }
                        load("");
                      } catch (e) {
                        setError(String(e));
                      }
                    }}
                    className="rounded border border-gray-700 px-2 py-0.5 text-xs text-gray-400 hover:border-ultron-accent hover:text-ultron-accent"
                  >
                    {s.status === "paused" ? "Resume" : "Pause"}
                  </button>
                  <button
                    onClick={async () => {
                      if (!confirm(`Delete skill "${s.name}"?`)) return;
                      try {
                        await api.deleteSkill(s.id);
                        load("");
                      } catch (e) {
                        setError(String(e));
                      }
                    }}
                    className="rounded border border-gray-700 px-2 py-0.5 text-xs text-red-400 hover:border-red-500"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </li>
          ))}
          {skills.length === 0 && !error && (
            <li className="text-sm text-gray-500">No skills learned yet — teach one above.</li>
          )}
        </ul>
      </section>
    </div>
  );
}
