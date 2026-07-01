import { useState } from "react";
import { api } from "../../lib/api";
import { humanizeError } from "../../lib/errorUtils";

export default function CodeView() {
  const [prompt, setPrompt] = useState("");
  const [name, setName] = useState("project");
  const [lang, setLang] = useState("Node.js");
  const [busy, setBusy] = useState(false);
  const [res, setRes] = useState<{ projectDir: string; files: string[]; notes: string | null } | null>(null);
  const [fileContents, setFileContents] = useState<Record<string, string>>({});
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  async function gen() {
    if (!prompt.trim()) return;
    setBusy(true); setErr(null); setFileContents({}); setSelectedFile(null);
    try { 
      const result = await api.codeGenerate(prompt, name, lang);
      setRes(result);
      
      // Try to open in VS Code if available
      tryOpenVSCode(result.projectDir);
      
      // Fetch file contents to display
      await loadFileContents(result.projectDir, result.files);
    }
    catch (e) { setErr(humanizeError(e)); } 
    finally { setBusy(false); }
  }
  
  function tryOpenVSCode(dir: string) {
    // Try to open VS Code via the URL protocol
    // This will only work if VS Code is installed and the protocol handler is registered
    try {
      const vscodeUrl = `vscode://file/${dir}`;
      window.open(vscodeUrl, '_blank');
    } catch (e) {
      console.log("VS Code auto-open not available:", e);
    }
  }
  
  async function loadFileContents(projectDir: string, files: string[]) {
    // For now, show a note that files are written to disk
    // In a full implementation, we'd read the actual file contents via backend endpoint
    const contents: Record<string, string> = {};
    for (const file of files) {
      contents[file] = `// File written to: ${projectDir}/${file}\n// Open in your editor to view and edit`;
    }
    setFileContents(contents);
    if (files.length > 0) setSelectedFile(files[0]);
  }

  return (
    <div className="space-y-4">
      <h2 className="font-mono text-[11px] tracking-[0.25em] text-[#00e5ff]">CODE GENERATOR</h2>
      
      <div className="rounded-lg border border-gray-700 bg-ultron-panel p-4">
        <textarea value={prompt} onChange={(e) => setPrompt(e.target.value)} rows={3}
          placeholder="e.g. build a REST API for a todo app with Express and SQLite"
          className="w-full rounded border border-[#00e5ff]/25 bg-black/50 p-3 font-mono text-sm text-[#bfefff] outline-none focus:border-[#00e5ff] placeholder:text-gray-600" />
        
        <div className="mt-3 flex gap-2">
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="project-name"
            className="flex-1 rounded border border-[#00e5ff]/25 bg-black/50 px-3 py-2 font-mono text-sm text-[#bfefff] outline-none" />
          <select value={lang} onChange={(e) => setLang(e.target.value)}
            className="rounded border border-[#00e5ff]/25 bg-black/50 px-3 py-2 font-mono text-sm text-[#bfefff] outline-none">
            <option>Node.js</option>
            <option>Python</option>
            <option>Java</option>
            <option>Go</option>
            <option>Rust</option>
            <option>TypeScript</option>
          </select>
          <button onClick={gen} disabled={busy}
            className="rounded border border-[#00e5ff]/50 bg-[#00e5ff]/10 px-6 py-2 font-mono text-sm font-bold tracking-wider text-[#00e5ff] hover:bg-[#00e5ff] hover:text-black disabled:opacity-50 transition-colors">
            {busy ? "GENERATING…" : "GENERATE"}
          </button>
        </div>
      </div>

      {err && <pre className="whitespace-pre-wrap rounded bg-red-950/50 p-4 text-xs leading-relaxed text-ultron-danger font-mono">{err}</pre>}
      
      {res && (
        <div className="space-y-3">
          <div className="rounded-lg border border-emerald-500/30 bg-emerald-950/20 p-3">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-sm text-emerald-400">✓ Project generated</div>
                <div className="mt-1 font-mono text-xs text-emerald-200/70">{res.projectDir}</div>
              </div>
              <button
                onClick={() => tryOpenVSCode(res.projectDir)}
                className="rounded border border-emerald-500/40 bg-emerald-900/30 px-3 py-1 font-mono text-xs text-emerald-300 hover:bg-emerald-900/50"
              >
                OPEN IN VS CODE
              </button>
            </div>
            {res.notes && <pre className="mt-2 whitespace-pre-wrap text-xs text-emerald-200/80">{res.notes}</pre>}
          </div>
          
          {res.files.length > 0 && (
            <div className="rounded-lg border border-gray-700 bg-ultron-panel">
              <div className="border-b border-gray-700 p-2">
                <div className="flex flex-wrap gap-1">
                  {res.files.map((file) => (
                    <button
                      key={file}
                      onClick={() => setSelectedFile(file)}
                      className={`rounded px-2 py-1 font-mono text-xs transition-colors ${
                        selectedFile === file
                          ? 'bg-[#00e5ff]/20 text-[#00e5ff] border border-[#00e5ff]/40'
                          : 'bg-black/40 text-gray-400 hover:text-gray-200'
                      }`}
                    >
                      {file.split('/').pop()}
                    </button>
                  ))}
                </div>
              </div>
              
              {selectedFile && fileContents[selectedFile] && (
                <div className="p-4">
                  <div className="mb-2 font-mono text-xs text-gray-500">{selectedFile}</div>
                  <pre className="overflow-x-auto rounded bg-black/60 p-3 font-mono text-xs text-gray-300 leading-relaxed">
                    {fileContents[selectedFile]}
                  </pre>
                  <div className="mt-3 text-xs text-amber-200/70">
                    ℹ Files have been written to disk. Open the project folder to view and edit the generated code.
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
