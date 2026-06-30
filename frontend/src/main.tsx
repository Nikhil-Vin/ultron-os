import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css";

class ErrorBoundary extends React.Component<{ children: React.ReactNode }, { error: Error | null }> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { error: null };
  }
  static getDerivedStateFromError(error: Error) {
    return { error };
  }
  componentDidCatch(error: Error, info: React.ErrorInfo) {
    // Surface in console for debugging.
    console.error("Ultron UI error:", error, info);
  }
  render() {
    if (this.state.error) {
      return (
        <div style={{ padding: 24, color: "#f87171", fontFamily: "monospace", background: "#000", height: "100vh" }}>
          <h2 style={{ color: "#00e5ff" }}>Ultron UI hit an error</h2>
          <pre style={{ whiteSpace: "pre-wrap" }}>{String(this.state.error?.stack || this.state.error)}</pre>
          <p style={{ color: "#8fd9ff" }}>Reload after fixing, or check the browser console.</p>
        </div>
      );
    }
    return this.props.children;
  }
}

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>
);
