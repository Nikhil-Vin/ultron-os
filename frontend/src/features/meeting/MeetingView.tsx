import { useState } from "react";
import { api } from "../../lib/api";

export default function MeetingView() {
  const [title, setTitle] = useState("");
  const [attendees, setAttendees] = useState("");
  const [brief, setBrief] = useState<any>(null);
  const [transcript, setTranscript] = useState("");
  const [actions, setActions] = useState<string[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function makeBrief() {
    setBusy(true); setError(null);
    try {
      setBrief(await api.meetingBrief(title || "Meeting", attendees.split(",").map((a) => a.trim()).filter(Boolean)));
    } catch (e) { setError(String(e)); } finally { setBusy(false); }
  }

  async function capture() {
    setBusy(true); setError(null);
    try {
      const r = await api.meetingCapture(title || "Meeting", transcript);
      setActions(r.actionItems);
    } catch (e) { setError(String(e)); } finally { setBusy(false); }
  }

  return (
    <div className="space-y-5">
      <section className="rounded-lg bg-ultron-panel p-4">
        <h2 className="mb-3 text-sm font-semibold text-gray-300">Pre-meeting brief</h2>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Meeting title"
          className="mb-2 w-full rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent" />
        <div className="flex gap-2">
          <input value={attendees} onChange={(e) => setAttendees(e.target.value)} placeholder="attendees (comma-separated)"
            className="flex-1 rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent" />
          <button onClick={makeBrief} disabled={busy}
            className="rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50">Brief me</button>
        </div>
        {brief && (
          <div className="mt-3 text-sm">
            <p className="text-gray-400">context items: {brief.contextItems}</p>
            <p className="mt-1 whitespace-pre-wrap text-gray-200">{brief.talkingPoints}</p>
          </div>
        )}
      </section>

      <section className="rounded-lg bg-ultron-panel p-4">
        <h2 className="mb-3 text-sm font-semibold text-gray-300">Post-call → action items</h2>
        <textarea value={transcript} onChange={(e) => setTranscript(e.target.value)} rows={4}
          placeholder="Paste the meeting transcript…"
          className="w-full rounded-md border border-gray-700 bg-ultron-bg p-2 text-sm text-gray-200 outline-none focus:border-ultron-accent" />
        <button onClick={capture} disabled={busy || !transcript.trim()}
          className="mt-2 rounded-lg bg-ultron-accent px-4 py-2 text-sm font-medium text-ultron-bg disabled:opacity-50">Extract action items</button>
        {actions && (
          <ul className="mt-3 space-y-1 text-sm text-gray-200">
            {actions.map((a, i) => <li key={i}>☐ {a}</li>)}
            {actions.length === 0 && <li className="text-gray-500">No action items found.</li>}
          </ul>
        )}
      </section>

      {error && <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>}
    </div>
  );
}
