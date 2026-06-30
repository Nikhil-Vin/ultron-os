"use client";

import { motion } from "framer-motion";
import {
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { MetricPoint } from "../lib/events";

export default function MetricsChart({
  title,
  unit,
  series,
}: {
  title: string;
  unit?: string;
  series: MetricPoint[];
}) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.97 }}
      animate={{ opacity: 1, scale: 1 }}
      className="rounded-2xl border border-ultron-accent/20 bg-ultron-panel/70 p-4 shadow-glow backdrop-blur"
    >
      <div className="mb-2 flex items-baseline justify-between">
        <span className="text-sm font-medium text-gray-200">{title}</span>
        {unit && <span className="text-xs text-gray-500">{unit}</span>}
      </div>
      <div className="h-44">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={series} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
            <XAxis dataKey="label" tick={{ fill: "#64748b", fontSize: 11 }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fill: "#64748b", fontSize: 11 }} axisLine={false} tickLine={false} width={40} />
            <Tooltip
              contentStyle={{ background: "#0d1424", border: "1px solid #1e293b", borderRadius: 8, color: "#e5e7eb" }}
            />
            <Line
              type="monotone"
              dataKey="value"
              stroke="#38bdf8"
              strokeWidth={2.5}
              dot={{ r: 3, fill: "#0ea5e9" }}
              isAnimationActive
              animationDuration={1100}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </motion.div>
  );
}
