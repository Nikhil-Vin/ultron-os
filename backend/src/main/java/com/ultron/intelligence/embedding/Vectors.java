package com.ultron.intelligence.embedding;

import java.util.StringJoiner;

/**
 * Small, dependency-free vector math used by the RAG layer (L3).
 *
 * <p>Embeddings are persisted as a compact CSV string so the same representation works on both
 * Postgres and the in-memory H2 used by tests — semantic ranking is computed in Java (cosine),
 * which keeps RAG fully offline. pgvector remains the drop-in production accelerator (the
 * extension is enabled in {@code V1__init.sql}); this utility is the portable fallback.
 */
public final class Vectors {

    private Vectors() {
    }

    /** Cosine similarity in [-1, 1]. Returns 0 for null/empty/mismatched-length inputs. */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** L2-normalise in place and return the same array (no-op for a zero vector). */
    public static float[] l2normalize(float[] v) {
        if (v == null || v.length == 0) {
            return v;
        }
        double sum = 0.0;
        for (float x : v) {
            sum += (double) x * x;
        }
        if (sum == 0.0) {
            return v;
        }
        double norm = Math.sqrt(sum);
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (v[i] / norm);
        }
        return v;
    }

    /** Serialise to a compact CSV string for persistence. {@code null}/empty → empty string. */
    public static String toCsv(float[] v) {
        if (v == null || v.length == 0) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(",");
        for (float x : v) {
            joiner.add(Float.toString(x));
        }
        return joiner.toString();
    }

    /** Parse a CSV embedding produced by {@link #toCsv(float[])}. Blank → empty array. */
    public static float[] fromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return new float[0];
        }
        String[] parts = csv.split(",");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException ex) {
                out[i] = 0f;
            }
        }
        return out;
    }
}
