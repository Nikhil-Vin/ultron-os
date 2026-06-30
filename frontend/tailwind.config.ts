import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ultron: {
          bg: "#0a0e1a",
          panel: "#111827",
          accent: "#38bdf8",
          danger: "#f87171",
        },
      },
    },
  },
  plugins: [],
} satisfies Config;
