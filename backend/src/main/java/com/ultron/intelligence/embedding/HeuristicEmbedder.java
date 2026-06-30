package com.ultron.intelligence.embedding;

import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * The offline-first fallback embedder (L3). Always available, no model, no network.
 *
 * <p>It maps text to a fixed-dimension vector using a deterministic hashed bag-of-words: each
 * token is hashed into a bucket and accumulated, then the vector is L2-normalised. Cosine
 * similarity between two such vectors grows with shared vocabulary, which is enough for useful
 * semantic-ish recall with zero dependencies — the fail-safe degradation target for RAG.
 */
@Component
public class HeuristicEmbedder implements Embedder {

    /** Fixed embedding width. Wide enough to keep hash collisions low for short documents. */
    public static final int DIMENSION = 512;

    @Override
    public String name() {
        return "heuristic";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    @Override
    public float[] embed(String text) {
        float[] vec = new float[DIMENSION];
        if (text == null || text.isBlank()) {
            return vec;
        }
        for (String token : tokenize(text)) {
            int bucket = Math.floorMod(token.hashCode(), DIMENSION);
            vec[bucket] += 1f;
        }
        return Vectors.l2normalize(vec);
    }

    /** Lowercase, split on non-alphanumeric, drop very short tokens. */
    private static String[] tokenize(String text) {
        String[] raw = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        int kept = 0;
        for (String t : raw) {
            if (t.length() >= 2) {
                kept++;
            }
        }
        String[] tokens = new String[kept];
        int i = 0;
        for (String t : raw) {
            if (t.length() >= 2) {
                tokens[i++] = t;
            }
        }
        return tokens;
    }
}
