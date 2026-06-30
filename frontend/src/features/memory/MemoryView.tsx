import { useEffect, useState } from "react";
import { api, type MemoryDto } from "../../lib/api";

export default function MemoryView() {
  const [query, setQuery] = useState("");
  const [content, setContent] = useState("");
  const [tags, setTags] = useState("");
  const [memories, setMemories] = useState<MemoryDto[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function load(q = query) {
    setError(null);
    try {
      setMemories(await api.recall(q));
    } catch (e) {
      setError(String(e));
    }
  }

  useEffect(() => {
    load("");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function save() {
    if (!content.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await api.saveMemory({ content, tags: tags || undefined, source: "ui" });
      setContent("");
      setTags("");
      await load("");
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-6">
      <section className="rounded-lg bg-ultron-panel p-4">
        <h2 className="mb-3 text-sm font-semibold text-gray-300">Capture a memory</h2>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="What should Ultron remember?"
          rows={3}
          className="w-full rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
        />
        <div className="mt-2 flex gap-2">
          <input
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            placeholder="tags (comma-separated)"
            className="flex-1 rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
          />
          <button
            onClick={save}
            disabled={busy || !content.trim()}
            className="rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50"
          >
            Save
          </button>
        </div>
      </section>

      <section>
        <div className="mb-3 flex gap-2">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && load()}
            placeholder="Search memories…"
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
          {memories.map((m) => (
            <li key={m.id} className="rounded-lg bg-ultron-panel p-3 text-sm">
              <p className="text-gray-200">{m.content}</p>
              <p className="mt-1 text-xs text-gray-500">
                {m.type} · {m.tags ?? "no tags"} ·{" "}
                {new Date(m.createdAt).toLocaleString()}
              </p>
            </li>
          ))}
          {memories.length === 0 && !error && (
            <li className="text-sm text-gray-500">No memories yet.</li>
          )}
        </ul>
      </section>
    </div>
  );
}
