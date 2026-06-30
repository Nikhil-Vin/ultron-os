package com.ultron.intelligence.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Chooses the active {@link Embedder} at call time (L3), mirroring
 * {@link com.ultron.intelligence.BrainSelector}. Prefers {@link OllamaEmbedder} when enabled and
 * reachable; otherwise degrades to the always-available {@link HeuristicEmbedder}.
 */
@Component
public class EmbedderSelector {

    private static final Logger log = LoggerFactory.getLogger(EmbedderSelector.class);

    private final OllamaEmbedder ollamaEmbedder;
    private final HeuristicEmbedder heuristicEmbedder;

    public EmbedderSelector(OllamaEmbedder ollamaEmbedder, HeuristicEmbedder heuristicEmbedder) {
        this.ollamaEmbedder = ollamaEmbedder;
        this.heuristicEmbedder = heuristicEmbedder;
    }

    /** The embedder that will actually serve right now. */
    public Embedder active() {
        if (ollamaEmbedder.isAvailable()) {
            return ollamaEmbedder;
        }
        return heuristicEmbedder;
    }

    /**
     * Embed using the active embedder, falling back to the always-available heuristic embedder if
     * the active one errors or returns an empty vector. This guarantees a usable embedding so that
     * memory/skill saves and RAG never fail because a model can't embed (Section 4 fail-safe).
     */
    public float[] embed(String text) {
        Embedder embedder = active();
        if (embedder != heuristicEmbedder) {
            try {
                float[] vec = embedder.embed(text);
                if (vec != null && vec.length > 0) {
                    return vec;
                }
                log.debug("Embedder={} returned empty; falling back to heuristic", embedder.name());
            } catch (RuntimeException ex) {
                log.warn("Embedder={} failed ({}); falling back to heuristic", embedder.name(), ex.getMessage());
            }
            return heuristicEmbedder.embed(text);
        }
        return embedder.embed(text);
    }

    /** Health-facing status of the embedding layer (no secrets). */
    public Status status() {
        boolean ollamaUp = ollamaEmbedder.isAvailable();
        return new Status(ollamaUp ? ollamaEmbedder.name() : heuristicEmbedder.name(),
            ollamaUp ? ollamaEmbedder.model() : "hashing-512", ollamaUp);
    }

    /**
     * Embedding-layer status for {@code /api/health}.
     *
     * @param active        the embedder that will serve now ({@code ollama} or {@code heuristic})
     * @param model         the embedding model in use
     * @param ollamaActive  true when a real Ollama embedding model is reachable and serving
     */
    public record Status(String active, String model, boolean ollamaActive) {
    }
}
