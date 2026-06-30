package com.ultron.skills;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A learned skill (L4 — The Moat). Skill Intake lets the owner "teach" Ultron any procedure or
 * knowledge by feeding it a name + description + instructions; it is embedded on intake so the
 * RAG layer can retrieve it as grounding context for reasoning (L3).
 */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2_000)
    private String description;

    /** Free-form skill body: the steps, knowledge, or playbook being taught. */
    @Column(name = "content", nullable = false, length = 20_000)
    private String content;

    /** Comma-separated tags. */
    @Column(name = "tags")
    private String tags;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Lifecycle status: {@code active} (default), {@code paused} (excluded from search+RAG). */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "active";

    /** CSV-encoded dense embedding for semantic retrieval (RAG, L3). */
    @Column(name = "embedding", length = 100_000)
    private String embedding;

    protected Skill() {
        // for JPA
    }

    public Skill(UUID id, String name, String description, String content,
                 String tags, String source, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.content = content;
        this.tags = tags;
        this.source = source;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }

    public String getTags() {
        return tags;
    }

    public String getSource() {
        return source;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
