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

    /**
     * Stream a reply token-by-token. Default: produce the full reply via {@link #think(String)} and
     * emit it as a single chunk. Streaming providers (Ollama, OpenAI-compatible, Anthropic, Gemini)
     * override this for real-time token delivery.
     *
     * @param prompt  the input prompt
     * @param onToken consumer invoked with each token/fragment as it arrives
     * @return the full concatenated reply
     */
    default String streamThink(String prompt, java.util.function.Consumer<String> onToken) {
        String answer = think(prompt);
        if (answer != null && !answer.isBlank()) {
            onToken.accept(answer);
        }
        return answer;
    }
}
