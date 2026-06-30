package com.ultron.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A single unit of Ultron's growing memory (L4 — The Moat).
 *
 * <p>Phase 0 stored plain content + metadata and recalled by keyword. Phase 1 adds the
 * {@code embedding} column (a CSV-encoded dense vector) for semantic recall via the RAG layer;
 * keyword recall remains as the fail-safe fallback.
 */
@Entity
@Table(name = "memories")
public class Memory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "content", nullable = false, length = 10_000)
    private String content;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "source", length = 100)
    private String source;

    /** Comma-separated tags; normalised into a join table in a later phase. */
    @Column(name = "tags")
    private String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** CSV-encoded dense embedding for semantic recall (RAG, L3). Nullable for legacy rows. */
    @Column(name = "embedding", length = 100_000)
    private String embedding;

    protected Memory() {
        // for JPA
    }

    public Memory(UUID id, String content, String type, String source, String tags, Instant createdAt) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.source = source;
        this.tags = tags;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
}
