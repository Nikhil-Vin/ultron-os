// Thin client for the Ultron backend. In dev, Vite proxies /api to localhost:8080.
const BASE = import.meta.env.VITE_API_BASE ?? "";

/** The optional API key (set when the backend's ULTRON_API_KEY gate is enabled). */
export function getApiKey(): string {
  if (typeof localStorage !== "undefined") {
    const k = localStorage.getItem("ultron.apiKey");
    if (k) return k;
  }
  return (import.meta as any).env?.VITE_ULTRON_KEY ?? "";
}
export function setApiKey(key: string) {
  if (typeof localStorage !== "undefined") localStorage.setItem("ultron.apiKey", key);
}

// One-time global fetch patch: attach X-Ultron-Key to every /api/* request when a key is configured.
if (typeof window !== "undefined" && !(window as any).__ultronFetchPatched) {
  (window as any).__ultronFetchPatched = true;
  const orig = window.fetch.bind(window);
  window.fetch = (input: RequestInfo | URL, init: RequestInit = {}) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : (input as Request).url;
    const key = getApiKey();
    if (key && url && url.includes("/api/")) {
      const headers = new Headers(init.headers || {});
      headers.set("X-Ultron-Key", key);
      init = { ...init, headers };
    }
    return orig(input as any, init);
  };
}

export interface Health {
  status: string;
  brain: string;
  brainModel?: string;
  llmActive?: boolean;
  embedder: string;
  embedderModel?: string;
  github: string;
  workers: string[];
  autoApprove: boolean;
}

export interface BriefResponse {
  success: boolean;
  brief: string;
  detail: string | null;
}

export interface MemoryDto {
  id: string;
  content: string;
  type: string;
  source: string | null;
  tags: string | null;
  createdAt: string;
}

export interface SkillDto {
  id: string;
  name: string;
  description: string | null;
  content: string;
  tags: string | null;
  source: string | null;
  status: string | null;
  createdAt: string;
}

export interface RetrievedItem {
  kind: string; // "memory" | "skill"
  id: string;
  title: string;
  content: string;
  score: number;
}

export interface AskResponse {
  answer: string;
  context: RetrievedItem[];
}

export interface AgentTrace {
  instruction: string;
  perceived: RetrievedItem[];
  reasoning: string;
  worker: string;
  kind: string;
  risk: string; // READ | LOW | HIGH | CRITICAL
  decision: string; // AUTO | APPROVED | DENIED
  auditId: string;
  acted: boolean;
  result: string;
}

async function json<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  health(): Promise<Health> {
    return fetch(`${BASE}/api/health`).then((r) => json<Health>(r));
  },

  brief(): Promise<BriefResponse> {
    return fetch(`${BASE}/api/brief`, { method: "POST" }).then((r) =>
      json<BriefResponse>(r)
    );
  },

  saveMemory(input: {
    content: string;
    type?: string;
    source?: string;
    tags?: string;
  }): Promise<MemoryDto> {
    return fetch(`${BASE}/api/memory`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(input),
    }).then((r) => json<MemoryDto>(r));
  },

  recall(query: string): Promise<MemoryDto[]> {
    const q = query ? `?q=${encodeURIComponent(query)}` : "";
    return fetch(`${BASE}/api/memory${q}`).then((r) => json<MemoryDto[]>(r));
  },

  listSkills(query?: string): Promise<SkillDto[]> {
    const q = query ? `?q=${encodeURIComponent(query)}` : "";
    return fetch(`${BASE}/api/skills${q}`).then((r) => json<SkillDto[]>(r));
  },

  intakeSkill(input: {
    name: string;
    description?: string;
    content: string;
    tags?: string;
    source?: string;
  }): Promise<SkillDto> {
    return fetch(`${BASE}/api/skills`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(input),
    }).then((r) => json<SkillDto>(r));
  },

  ingestSkillFromSource(input: {
    name: string;
    description?: string;
    url: string;
    contentType?: string;
  }): Promise<SkillDto> {
    return fetch(`${BASE}/api/skills/ingest`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(input),
    }).then((r) => json<SkillDto>(r));
  },

  deleteSkill(id: string): Promise<void> {
    return fetch(`${BASE}/api/skills/${id}`, { method: "DELETE" }).then((r) => {
      if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
    });
  },

  pauseSkill(id: string): Promise<SkillDto> {
    return fetch(`${BASE}/api/skills/${id}/pause`, { method: "POST" }).then((r) =>
      json<SkillDto>(r)
    );
  },

  resumeSkill(id: string): Promise<SkillDto> {
    return fetch(`${BASE}/api/skills/${id}/resume`, { method: "POST" }).then((r) =>
      json<SkillDto>(r)
    );
  },

  testSkillRetrieval(id: string, query: string): Promise<{ id: string; name: string; score: number; contentPreview: string }> {
    return fetch(`${BASE}/api/skills/${id}/test`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ query }),
    }).then((r) => json(r));
  },

  ask(question: string, topK = 5): Promise<AskResponse> {
    return fetch(`${BASE}/api/ask`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ question, topK }),
    }).then((r) => json<AskResponse>(r));
  },

  runAgent(instruction: string): Promise<AgentTrace> {
    return fetch(`${BASE}/api/agent`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ instruction }),
    }).then((r) => json<AgentTrace>(r));
  },

  approveAgent(instruction: string): Promise<AgentTrace> {
    return fetch(`${BASE}/api/agent/approve`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ instruction }),
    }).then((r) => json<AgentTrace>(r));
  },

  // --- Trading (Phase 3) ---
  tradingStatus(): Promise<any> {
    return fetch(`${BASE}/api/trading/status`).then((r) => json(r));
  },

  tradingSignal(instrument: string, indicators: Record<string, number>): Promise<TradingAdvice> {
    return fetch(`${BASE}/api/trading/signal`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ instrument, indicators }),
    }).then((r) => json<TradingAdvice>(r));
  },

  tradingSignals(): Promise<TradingSignalDto[]> {
    return fetch(`${BASE}/api/trading/signals`).then((r) => json<TradingSignalDto[]>(r));
  },

  tradingJournal(): Promise<{ trades: TradeDto[]; performance: Performance }> {
    return fetch(`${BASE}/api/trading/journal`).then((r) => json(r));
  },

  tradingPsychology(): Promise<PsychAssessment> {
    return fetch(`${BASE}/api/trading/psychology`).then((r) => json<PsychAssessment>(r));
  },

  paperTrade(input: {
    instrument: string;
    side: string;
    quantity: number;
    stop?: number;
    target?: number;
    signalSource?: string;
  }): Promise<TradeDto> {
    return fetch(`${BASE}/api/trading/paper-trade`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(input),
    }).then((r) => json<TradeDto>(r));
  },

  // --- Connectors / Meeting / Notifications / Finance (Phase 4) ---
  connectorsStatus(): Promise<any> {
    return fetch(`${BASE}/api/connectors`).then((r) => json(r));
  },

  activateScene(scene: string, approved: boolean): Promise<any> {
    return fetch(`${BASE}/api/connectors/scene`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ scene, approved }),
    }).then((r) => json(r));
  },

  meetingBrief(title: string, attendees: string[]): Promise<any> {
    return fetch(`${BASE}/api/meeting/brief`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ title, attendees }),
    }).then((r) => json(r));
  },

  meetingCapture(title: string, transcript: string): Promise<{ title: string; actionItems: string[] }> {
    return fetch(`${BASE}/api/meeting/capture`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ title, transcript }),
    }).then((r) => json(r));
  },

  routeNotification(level: string, source: string, text: string): Promise<any> {
    return fetch(`${BASE}/api/notifications/route`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ level, source, text }),
    }).then((r) => json(r));
  },

  financeOverview(holdings: { name: string; value: number }[], monthlySpend: number, budget: number): Promise<any> {
    return fetch(`${BASE}/api/finance/overview`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ holdings, monthlySpend, budget }),
    }).then((r) => json(r));
  },

  // --- Voice profile (syncs the backend voice agent) ---
  voiceStatus(): Promise<any> {
    return fetch(`${BASE}/api/voice/status`).then((r) => json(r));
  },

  setVoiceProfile(id: string): Promise<any> {
    return fetch(`${BASE}/api/voice/profile`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ id }),
    }).then((r) => json(r));
  },

  // --- Brain providers ---
  brainProviders(): Promise<{ active: string; activeModel: string; llmActive: boolean; pinned: string; providers: { name: string; model: string; available: boolean; active: boolean }[] }> {
    return fetch(`${BASE}/api/brain/providers`).then((r) => json(r));
  },

  selectBrain(provider: string): Promise<any> {
    return fetch(`${BASE}/api/brain/select`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ provider }),
    }).then((r) => json(r));
  },

  setLanguage(code: string): Promise<any> {
    return fetch(`${BASE}/api/languages/active`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ code }),
    }).then((r) => json(r));
  },

  devices(): Promise<{ id: string; type: string; online: boolean; capabilities: string[] }[]> {
    return fetch(`${BASE}/api/devices`).then((r) => json(r));
  },

  systemStats(): Promise<{ cpuPercent: number; heapPercent: number; heapUsedMb: number; processors: number; uptimeSeconds: number; memories: number; skills: number; brain: string }> {
    return fetch(`${BASE}/api/system`).then((r) => json(r));
  },

  deviceCommand(text: string, approved = false, similarity?: number): Promise<any> {
    return fetch(`${BASE}/api/devices/command`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ text, approved, similarity }),
    }).then((r) => json(r));
  },
};

export interface TradingSignalDto {
  id: string;
  instrument: string;
  signalType: string;
  confidence: number;
  reasoning: string;
  sentimentScore: number;
  createdAt: string;
}

export interface TradingAdvice {
  signal: TradingSignalDto;
  narrative: string;
  rulesApplied: number;
  knowledgeUsed: number;
}

export interface TradeDto {
  id: string;
  instrument: string;
  tradeType: string;
  quantity: number;
  entryPrice: number | null;
  pnl: number | null;
  riskReward: number | null;
  executionMode: string;
  psychologyFlags: string | null;
  createdAt: string;
}

export interface Performance {
  totalTrades: number;
  closedTrades: number;
  wins: number;
  winRate: number;
  totalPnl: number;
  avgRiskReward: number;
}

export interface PsychAssessment {
  flags: string[];
  disciplineScore: number;
  tradesToday: number;
  recentLosses: number;
}
