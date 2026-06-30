// Ultron Capture — background service worker (MV3).
// Receives extracted page content from the popup/content script and sends it to the Ultron backend
// as a skill (READ/LOW). The backend's ApprovalGate governs anything consequential downstream.

const DEFAULT_BACKEND = "http://localhost:8080";

async function backendUrl() {
  const { backend } = await chrome.storage.local.get("backend");
  return backend || DEFAULT_BACKEND;
}

async function captureToSkill({ title, url, text }) {
  const base = await backendUrl();
  const { ultronKey } = await chrome.storage.local.get("ultronKey");
  const headers = { "content-type": "application/json" };
  if (ultronKey) headers["X-Ultron-Key"] = ultronKey;
  const body = {
    name: (title || url || "Captured page").slice(0, 180),
    description: `Captured from ${url}`,
    content: (text || "").slice(0, 18000),
    tags: "web-capture",
    source: url,
  };
  const res = await fetch(`${base}/api/skills`, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`backend ${res.status}`);
  return res.json();
}

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg?.type === "capture") {
    captureToSkill(msg.payload)
      .then((skill) => sendResponse({ ok: true, skill }))
      .catch((e) => sendResponse({ ok: false, error: String(e) }));
    return true; // async response
  }
  return false;
});
