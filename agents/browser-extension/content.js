// Ultron Capture — content script. Extracts the current page's readable text when asked.
// Read-only: it only reads the DOM and never mutates the page or auto-sends anything.

function extractReadable() {
  // Prefer <article>/<main> if present, else the body.
  const root = document.querySelector("article") || document.querySelector("main") || document.body;
  const text = (root?.innerText || "").replace(/\s+\n/g, "\n").trim();
  return { title: document.title, url: location.href, text };
}

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg?.type === "extract") {
    sendResponse(extractReadable());
  }
  return false;
});
