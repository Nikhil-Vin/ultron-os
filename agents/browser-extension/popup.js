// Ultron Capture — popup logic.
const statusEl = document.getElementById("status");
const backendEl = document.getElementById("backend");
const keyEl = document.getElementById("ultronKey");
const captureBtn = document.getElementById("capture");

(async () => {
  const { backend, ultronKey } = await chrome.storage.local.get(["backend", "ultronKey"]);
  if (backend) backendEl.value = backend;
  if (ultronKey) keyEl.value = ultronKey;
})();

backendEl.addEventListener("change", () => {
  chrome.storage.local.set({ backend: backendEl.value.trim() });
});
keyEl.addEventListener("change", () => {
  chrome.storage.local.set({ ultronKey: keyEl.value.trim() });
});

captureBtn.addEventListener("click", async () => {
  captureBtn.disabled = true;
  statusEl.textContent = "Reading page…";
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    const page = await chrome.tabs.sendMessage(tab.id, { type: "extract" });
    statusEl.textContent = "Sending to Ultron…";
    const res = await chrome.runtime.sendMessage({ type: "capture", payload: page });
    statusEl.textContent = res?.ok
      ? `Captured “${res.skill?.name ?? "page"}”.`
      : `Error: ${res?.error ?? "unknown"}`;
  } catch (e) {
    statusEl.textContent = `Error: ${e}`;
  } finally {
    captureBtn.disabled = false;
  }
});
