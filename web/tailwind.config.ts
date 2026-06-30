import type { Config } from "tailwindcss";

export default {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ultron: {
          bg: "#05070f",
          panel: "#0d1424",
          accent: "#38bdf8",
          glow: "#0ea5e9",
          danger: "#f87171",
        },
      },
      boxShadow: {
        glow: "0 0 40px rgba(56,189,248,0.25)",
      },
    },
  },
  plugins: [],
} satisfies Config;
