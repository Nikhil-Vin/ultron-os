"use client";

import { motion } from "framer-motion";
import { PipelineStage } from "../lib/events";

export default function PipelineFunnel({ title, stages }: { title: string; stages: PipelineStage[] }) {
  const max = Math.max(1, ...stages.map((s) => s.value));
  return (
    <div className="rounded-2xl border border-ultron-accent/20 bg-ultron-panel/70 p-4 shadow-glow backdrop-blur">
      <div className="mb-3 text-sm font-medium text-gray-200">{title}</div>
      <div className="space-y-2">
        {stages.map((s, i) => (
          <div key={i} className="flex items-center gap-3">
            <span className="w-24 text-xs text-gray-400">{s.label}</span>
            <div className="h-6 flex-1 overflow-hidden rounded-md bg-ultron-bg">
              <motion.div
                className="h-full rounded-md"
                style={{ background: s.atRisk ? "#f87171" : "#38bdf8" }}
                initial={{ width: 0 }}
                animate={{ width: `${(s.value / max) * 100}%` }}
                transition={{ duration: 0.9, delay: i * 0.1 }}
              />
            </div>
            <span className="w-8 text-right text-xs text-gray-300">{s.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
