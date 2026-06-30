"use client";

import { useEffect, useRef, useState } from "react";
import { AnimatePresence } from "framer-motion";
import type { Room } from "livekit-client";
import { connectHud, publishControl } from "../lib/livekitClient";
import { RenderEvent, VoiceState, MetricPoint, PipelineStage } from "../lib/events";
import SleepScreen from "../components/SleepScreen";
import VoiceBar from "../components/VoiceBar";
import BriefPanel from "../components/BriefPanel";
import MetricsChart from "../components/MetricsChart";
import PipelineFunnel from "../components/PipelineFunnel";

export default function Page() {
  const [connected, setConnected] = useState(false);
  const [awake, setAwake] = useState(false);
  const [state, setState] = useState<VoiceState>("idle");
  const [sentence, setSentence] = useState("");
  const [metrics, setMetrics] = useState<{ title: string; unit?: string; series: MetricPoint[] } | null>(null);
  const [pipeline, setPipeline] = useState<{ title: string; stages: PipelineStage[] } | null>(null);
  const [brief, setBrief] = useState<{ title: string; lines: string[] } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const roomRef = useRef<Room | null>(null);

  useEffect(() => {
    let mounted = true;
    connectHud((e: RenderEvent) => {
      if (!mounted) return;
      switch (e.type) {
        case "state":
          setState(e.payload.state as VoiceState);
          if (e.payload.state !== "idle") setAwake(true);
          break;
        case "sentence":
          setSentence(e.payload.text);
          setAwake(true);
          break;
        case "brief":
          setBrief(e.payload);
          break;
        case "metrics":
          setMetrics(e.payload);
          break;
        case "pipeline":
          setPipeline(e.payload);
          break;
        default:
          break;
      }
    })
      .then((room) => {
        roomRef.current = room;
        setConnected(true);
      })
      .catch((err) => setError(String(err)));

    return () => {
      mounted = false;
      roomRef.current?.disconnect();
    };
  }, []);

  function wake() {
    setAwake(true);
    if (roomRef.current) publishControl(roomRef.current, "wake").catch(() => {});
  }

  return (
    <main className="min-h-screen">
      <AnimatePresence mode="wait">
        {!awake ? (
          <SleepScreen key="sleep" onWake={wake} />
        ) : (
          <div key="hud" className="mx-auto max-w-5xl space-y-5 p-6">
            <header className="flex items-center justify-between">
              <div>
                <h1 className="text-xl font-semibold text-ultron-accent">Ultron HUD</h1>
                <p className="text-xs text-gray-500">
                  {connected ? "connected" : "connecting…"}
                  {error ? ` · ${error}` : ""}
                </p>
              </div>
              <VoiceBar state={state} />
            </header>

            <BriefPanel sentence={sentence} />

            <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
              {metrics && <MetricsChart title={metrics.title} unit={metrics.unit} series={metrics.series} />}
              {pipeline && <PipelineFunnel title={pipeline.title} stages={pipeline.stages} />}
            </div>

            {brief && (
              <div className="rounded-2xl border border-ultron-accent/20 bg-ultron-panel/70 p-5 backdrop-blur">
                <div className="mb-2 text-sm font-medium text-gray-200">{brief.title}</div>
                <ul className="space-y-1 text-sm text-gray-300">
                  {brief.lines.map((ln, i) => (
                    <li key={i}>• {ln}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </AnimatePresence>
    </main>
  );
}
