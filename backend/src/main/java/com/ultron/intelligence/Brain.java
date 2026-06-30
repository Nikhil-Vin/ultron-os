package com.ultron.intelligence;

/**
 * The reasoning surface (L3 — The Brain). Model-agnostic by design: implementations may be a
 * local heuristic, a local Ollama model, or (opt-in, later) a cloud LLM. Callers never depend
 * on a specific provider.
 */
public interface Brain {

    /** Stable identifier reported by {@code /api/health}, e.g. {@code heuristic} or {@code ollama}. */
    String name();

    /** True when this brain is ready to serve (e.g. its backing model is reachable). */
    boolean isAvailable();

    /**
     * Produce a reasoned reply to a prompt.
     *
     * @param prompt the input prompt
     * @return the generated text
     */
    String think(String prompt);
}
