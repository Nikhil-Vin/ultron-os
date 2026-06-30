// Thin client for the Ultron backend. In dev, Vite proxies /api to localhost:8080.
const BASE = import.meta.env.VITE_API_BASE ?? "";

export interface Health {
  status: string;
  brain: string;
  github: string;
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
};
