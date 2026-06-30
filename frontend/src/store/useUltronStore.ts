import { useSyncExternalStore } from "react";

/**
 * Minimal Zustand-compatible store with zero dependencies (uses React's useSyncExternalStore).
 * Swap for the real `zustand` package by replacing this file — the `create`/`useStore` API matches.
 */
type Listener = () => void;

export interface UltronState {
  workMode: string;
  voiceState: "idle" | "listening" | "thinking" | "speaking";
  setWorkMode: (m: string) => void;
  setVoiceState: (s: UltronState["voiceState"]) => void;
}

function createStore() {
  let state: UltronState;
  const listeners = new Set<Listener>();
  const set = (partial: Partial<UltronState>) => {
    state = { ...state, ...partial };
    listeners.forEach((l) => l());
  };
  state = {
    workMode: "CASUAL",
    voiceState: "idle",
    setWorkMode: (m) => set({ workMode: m }),
    setVoiceState: (s) => set({ voiceState: s }),
  };
  return {
    getState: () => state,
    subscribe: (l: Listener) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
  };
}

const store = createStore();

export function useUltronStore<T>(selector: (s: UltronState) => T): T {
  return useSyncExternalStore(store.subscribe, () => selector(store.getState()));
}

export const ultronStore = store;
