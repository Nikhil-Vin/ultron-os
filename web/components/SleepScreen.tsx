"use client";

import { motion } from "framer-motion";

/** Passive ambient display (Section 9.9.F). Wakes smoothly when voice activation occurs. */
export default function SleepScreen({ onWake }: { onWake: () => void }) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onWake}
      className="flex h-screen cursor-pointer flex-col items-center justify-center"
    >
      <motion.div
        className="h-40 w-40 rounded-full"
        style={{
          background: "radial-gradient(circle at 50% 50%, rgba(56,189,248,0.5), rgba(14,165,233,0.05) 70%)",
        }}
        animate={{ scale: [1, 1.08, 1], opacity: [0.7, 1, 0.7] }}
        transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
      />
      <p className="mt-10 text-sm uppercase tracking-[0.4em] text-ultron-accent/60">Ultron</p>
      <p className="mt-2 text-xs text-gray-600">say “Ultron” or tap to begin</p>
    </motion.div>
  );
}
