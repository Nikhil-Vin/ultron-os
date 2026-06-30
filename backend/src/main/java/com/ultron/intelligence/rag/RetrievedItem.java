package com.ultron.intelligence.rag;

/**
 * A single piece of grounding context returned by the {@link RagService} (L3).
 *
 * @param kind   {@code memory} or {@code skill}
 * @param id     source row id
 * @param title  short label (memory type or skill name)
 * @param content the text used as context
 * @param score  retrieval score (cosine similarity, or lexical overlap when no embedding exists)
 */
public record RetrievedItem(String kind, String id, String title, String content, double score) {
}
