package com.ultron.intelligence.embedding;

/**
 * The embedding surface (L3 — The Brain). Turns text into a dense vector for semantic recall.
 * Model-agnostic, mirroring {@link com.ultron.intelligence.Brain}: a local heuristic embedder is
 * always available, and a local Ollama embedder is used automatically when reachable.
 */
public interface Embedder {

    /** Stable identifier reported by {@code /api/health}, e.g. {@code heuristic} or {@code ollama}. */
    String name();

    /** True when this embedder is ready to serve. */
    boolean isAvailable();

    /** Dimensionality of vectors this embedder produces. */
    int dimension();

    /**
     * Embed text into a dense vector.
     *
     * @param text input text (null/blank yields a zero vector)
     * @return the embedding
     */
    float[] embed(String text);
}
