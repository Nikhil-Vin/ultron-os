import { useCallback, useState } from "react";
import { api } from "../../lib/api";

export default function SkillIntakeForm({ onLearned }: { onLearned: () => void }) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [content, setContent] = useState("");
  const [tags, setTags] = useState("");
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [okMsg, setOkMsg] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);

  // Text-based intake
  const canSubmitText = name.trim().length > 0 && content.trim().length > 0 && !busy;
  // URL/file-based intake
  const canSubmitSource = name.trim().length > 0 && url.trim().length > 0 && !busy;

  async function submitText() {
    if (!canSubmitText) return;
    setBusy(true);
    setError(null);
    setOkMsg(null);
    try {
      const skill = await api.intakeSkill({
        name,
        description: description || undefined,
        content,
        tags: tags || undefined,
        source: "ui",
      });
      setOkMsg(`Learned "${skill.name}".`);
      resetForm();
      onLearned();
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function submitSource() {
    if (!canSubmitSource) return;
    setBusy(true);
    setError(null);
    setOkMsg(null);
    try {
      const skill = await api.ingestSkillFromSource({
        name,
        description: description || undefined,
        url,
      });
      setOkMsg(`Ingested "${skill.name}" from source.`);
      resetForm();
      onLearned();
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  function resetForm() {
    setName("");
    setDescription("");
    setContent("");
    setTags("");
    setUrl("");
  }

  // --- Drag and drop ---
  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFile(files[0]);
    }
  }, []);

  function handleFileInput(e: React.ChangeEvent<HTMLInputElement>) {
    const files = e.target.files;
    if (files && files.length > 0) {
      handleFile(files[0]);
    }
  }

  function handleFile(file: File) {
    const fileName = file.name;
    if (!name) {
      setName(fileName.replace(/\.[^.]+$/, ""));
    }

    // For text-based files, read content directly
    const textExts = [".txt", ".md", ".markdown", ".json", ".csv"];
    const isText = textExts.some((ext) => fileName.toLowerCase().endsWith(ext));

    if (isText) {
      const reader = new FileReader();
      reader.onload = (ev) => {
        const text = ev.target?.result;
        if (typeof text === "string") {
          setContent(text);
        }
      };
      reader.readAsText(file);
    } else {
      // For PDF/EPUB/other, set the filename as a reference
      // The real ingestion goes through the ai-layer via URL or upload endpoint
      setUrl(`file://${fileName}`);
      setContent(`[File: ${fileName} — ${(file.size / 1024).toFixed(1)} KB. Use "Ingest from source" to process via ai-layer.]`);
    }
  }

  return (
    <section className="rounded-lg bg-ultron-panel p-4">
      <h2 className="mb-3 text-sm font-semibold text-gray-300">Teach Ultron a skill</h2>

      {/* Drag and drop zone */}
      <div
        onDragEnter={handleDrag}
        onDragOver={handleDrag}
        onDragLeave={handleDrag}
        onDrop={handleDrop}
        className={`mb-3 flex cursor-pointer items-center justify-center rounded-lg border-2 border-dashed p-4 text-center transition-colors ${
          dragActive
            ? "border-ultron-accent bg-ultron-accent/10"
            : "border-gray-700 hover:border-gray-500"
        }`}
        role="button"
        tabIndex={0}
        aria-label="Drop a file here or click to upload"
        onClick={() => document.getElementById("skill-file-input")?.click()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            document.getElementById("skill-file-input")?.click();
          }
        }}
      >
        <input
          id="skill-file-input"
          type="file"
          accept=".txt,.md,.pdf,.epub,.json,.csv"
          onChange={handleFileInput}
          className="hidden"
        />
        <p className="text-sm text-gray-400">
          {dragActive
            ? "Drop file here…"
            : "Drag & drop a file (PDF, TXT, MD, EPUB, CSV) or click to browse"}
        </p>
      </div>

      <div className="space-y-2">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Skill name (e.g. Deploy frontend)"
          className="w-full rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
        />
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Short description (optional)"
          className="w-full rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
        />
        <input
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="Source URL (PDF link, YouTube, webpage — optional)"
          className="w-full rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
        />
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="The steps / knowledge to learn…"
          rows={4}
          className="w-full rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
        />
        <div className="flex gap-2">
          <input
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            placeholder="tags (comma-separated)"
            className="flex-1 rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent"
          />
          <button
            onClick={submitText}
            disabled={!canSubmitText}
            className="rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50"
          >
            {busy ? "Learning…" : "Learn skill"}
          </button>
          {url.trim() && (
            <button
              onClick={submitSource}
              disabled={!canSubmitSource}
              className="rounded-lg border border-ultron-accent px-4 py-2 text-sm font-medium text-ultron-accent disabled:opacity-50"
            >
              {busy ? "Ingesting…" : "Ingest source"}
            </button>
          )}
        </div>
      </div>

      {okMsg && <p className="mt-3 text-sm text-ultron-accent">{okMsg}</p>}
      {error && (
        <p className="mt-3 rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>
      )}
    </section>
  );
}
