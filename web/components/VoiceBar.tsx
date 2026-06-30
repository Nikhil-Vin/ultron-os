"use client";

import { motion } from "framer-motion";
import { VoiceState } from "../lib/events";

const STATE_LABEL: Record<VoiceState, string> = {
  idle: "Idle",
  listening: "Listening",
  thinking: "Thinking",
  speaking: "Speaking",
  interrupted: "Interrupted",
};

const STATE_COLOR: Record<VoiceState, string> = {
  idle: "#475569",
  listening: "#38bdf8",
  thinking: "#a78bfa",
  speaking: "#34d399",
  interrupted: "#f87171",
};

export default function VoiceBar({ state }: { state: VoiceState }) {
  const color = STATE_COLOR[state];
  const active = state !== "idle";
  const bars = 28;

  return (
    <div className="flex flex-col items-center gap-3">
      <div className="flex h-16 items-end gap-1">
        {Array.from({ length: bars }).map((_, i) => (
          <motion.div
            key={i}
            className="w-1.5 rounded-full"
            style={{ backgroundColor: color }}
            animate={{
              height: active ? [6, 10 + ((i * 13) % 44), 6] : 6,
              opacity: active ? 1 : 0.4,
            }}
            transition={{
              duration: state === "thinking" ? 1.1 : 0.6,
              repeat: Infinity,
              delay: (i % 7) * 0.05,
              ease: "easeInOut",
            }}
          />
        ))}
      </div>
      <div className="text-xs uppercase tracking-[0.25em]" style={{ color }}>
        {STATE_LABEL[state]}
      </div>
    </div>
  );
}
