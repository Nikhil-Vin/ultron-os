import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ultron: {
          bg: "#080808",
          panel: "#0e0e0e",
          accent: "#D4AF37",
          gold: "#D4AF37",
          obsidian: "#080808",
          danger: "#f87171",
        },
      },
      fontFamily: {
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      boxShadow: {
        glow: "0 0 25px rgba(212,175,55,0.2)",
        "glow-strong": "0 0 40px rgba(212,175,55,0.35)",
      },
    },
  },
  plugins: [],
} satisfies Config;
