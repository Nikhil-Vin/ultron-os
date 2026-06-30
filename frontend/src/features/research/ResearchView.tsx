import { useState } from "react";
import { api, type AskResponse } from "../../lib/api";

export default function ResearchView() {
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState<AskResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function ask() {
    if (!question.trim()) return;
    setBusy(true);
    setError(null);
    try {
      setAnswer(await api.ask(question));
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
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && ask()}
          placeholder="Ask, grounded in your memories + skills…"
          className="flex-1 rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
        />
        <button
          onClick={ask}
          disabled={busy || !question.trim()}
          className="rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50"
        >
          {busy ? "Thinking…" : "Ask"}
        </button>
      </div>

      {error && (
        <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>
      )}

      {answer && (
        <div className="space-y-4">
          <div className="rounded-lg bg-ultron-panel p-4">
            <h2 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
              Answer
            </h2>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-gray-200">
              {answer.answer}
            </p>
          </div>

          <div>
            <h2 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
              Grounding context ({answer.context.length})
            </h2>
            <ul className="space-y-2">
              {answer.context.map((item) => (
                <li key={`${item.kind}-${item.id}`} className="rounded-lg bg-ultron-panel p-3 text-sm">
                  <div className="mb-1 flex items-center justify-between">
                    <span className="text-ultron-accent">
                      {item.kind} · {item.title}
                    </span>
                    <span className="text-xs text-gray-500">
                      score {item.score.toFixed(3)}
                    </span>
                  </div>
                  <p className="text-gray-300">{item.content}</p>
                </li>
              ))}
              {answer.context.length === 0 && (
                <li className="text-sm text-gray-500">
                  No relevant memories or skills found — teach Ultron something first.
                </li>
              )}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}
