import { useEffect, useState } from "react";
import { api } from "../../lib/api";

export default function ConnectorSettingsView() {
  const [status, setStatus] = useState<any>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.connectorsStatus().then(setStatus).catch((e) => setError(String(e)));
  }, []);

  async function scene(name: string) {
    setMsg(null);
    try {
      const r = await api.activateScene(name, false); // approval required → expect blocked by default
      setMsg(r.connected ? `${name}: ${r.message}` : `${name}: not connected`);
    } catch (e) {
      setError(String(e));
    }
  }

  const rows: [string, boolean][] = status
    ? [
        ["Gmail", status.gmail],
        ["Calendar", status.calendar],
        ["Notion", status.notion],
        ["Slack", status.slack],
        ["Spotify", status.spotify],
        ["Home Assistant", status.homeassistant],
      ]
    : [];

  return (
    <div className="space-y-5">
      <section>
        <h2 className="mb-3 text-sm font-semibold text-gray-300">Connectors</h2>
        {error && <p className="rounded-md bg-red-950/50 p-3 text-sm text-ultron-danger">{error}</p>}
        <ul className="space-y-1">
          {rows.map(([name, ok]) => (
            <li key={name} className="flex items-center justify-between rounded-md bg-ultron-panel px-3 py-2 text-sm">
              <span className="text-gray-200">{name}</span>
              <span className={ok ? "text-emerald-400" : "text-gray-500"}>
                {ok ? "connected" : "not connected"}
              </span>
            </li>
          ))}
          {status?.twilio && (
            <li className="flex items-center justify-between rounded-md bg-ultron-panel px-3 py-2 text-sm">
              <span className="text-gray-200">Twilio (SMS/call)</span>
              <span className="text-ultron-danger">
                {status.twilio.configured ? (status.twilio.liveEnabled ? "live enabled" : "extension point (disabled)") : "not connected"}
              </span>
            </li>
          )}
        </ul>
        <p className="mt-2 text-xs text-gray-500">
          Sends, creates, and scenes require approval. Twilio is a disabled contact extension point.
        </p>
      </section>

      <section>
        <h2 className="mb-3 text-sm font-semibold text-gray-300">Scenes (Home Assistant)</h2>
        <div className="flex gap-2">
          {["Trading Mode", "Focus Mode", "End of Day"].map((s) => (
            <button
              key={s}
              onClick={() => scene(s)}
              className="rounded-lg border border-gray-700 px-3 py-2 text-sm text-gray-200 hover:border-ultron-accent"
            >
              {s}
            </button>
          ))}
        </div>
        {msg && <p className="mt-3 text-sm text-gray-400">{msg}</p>}
      </section>
    </div>
  );
}
