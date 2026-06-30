package com.ultron.intelligence.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pure unit test — no Spring, no network. Verifies the offline embedder is deterministic and that
 * cosine similarity reflects shared vocabulary (the property the RAG layer relies on).
 */
class EmbedderTest {

    private final HeuristicEmbedder embedder = new HeuristicEmbedder();

    @Test
    void embeddingIsDeterministicAndNormalised() {
        float[] a = embedder.embed("pgvector index tuning for large datasets");
        float[] b = embedder.embed("pgvector index tuning for large datasets");

        assertThat(a).hasSize(HeuristicEmbedder.DIMENSION);
        assertThat(a).containsExactly(b);                 // deterministic
        assertThat(Vectors.cosine(a, b)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void similarTextScoresHigherThanUnrelatedText() {
        float[] query = embedder.embed("how do I tune the pgvector index");
        float[] related = embedder.embed("pgvector index tuning guide for performance");
        float[] unrelated = embedder.embed("bought groceries and paid the electricity bill");

        double simRelated = Vectors.cosine(query, related);
        double simUnrelated = Vectors.cosine(query, unrelated);

        assertThat(simRelated).isGreaterThan(simUnrelated);
        assertThat(simRelated).isGreaterThan(0.0);
    }

    @Test
    void blankTextYieldsZeroVector() {
        float[] v = embedder.embed("   ");
        assertThat(v).hasSize(HeuristicEmbedder.DIMENSION);
        assertThat(Vectors.cosine(v, embedder.embed("anything"))).isEqualTo(0.0);
    }

    @Test
    void csvRoundTripsExactly() {
        float[] v = embedder.embed("round trip test content");
        float[] back = Vectors.fromCsv(Vectors.toCsv(v));
        assertThat(Vectors.cosine(v, back)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }
}
