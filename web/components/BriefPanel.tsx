"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";

/**
 * Letter-by-letter type-out of the current spoken sentence, timed to roughly match TTS playback
 * (Section 9.9.F). Each incoming `sentence` event replaces the typed line.
 */
export default function BriefPanel({ sentence }: { sentence: string }) {
  const [shown, setShown] = useState("");

  useEffect(() => {
    if (!sentence) return;
    setShown("");
    let i = 0;
    const id = setInterval(() => {
      i += 1;
      setShown(sentence.slice(0, i));
      if (i >= sentence.length) clearInterval(id);
    }, 28); // ~36 chars/sec, close to spoken cadence
    return () => clearInterval(id);
  }, [sentence]);

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="min-h-[96px] rounded-2xl border border-ultron-accent/20 bg-ultron-panel/70 p-5 shadow-glow backdrop-blur"
    >
      <div className="mb-2 text-xs uppercase tracking-[0.25em] text-ultron-accent/70">Ultron</div>
      <p className="text-lg leading-relaxed text-gray-100">
        {shown}
        <span className="ml-0.5 inline-block h-5 w-0.5 animate-pulse bg-ultron-accent align-middle" />
      </p>
    </motion.div>
  );
}
