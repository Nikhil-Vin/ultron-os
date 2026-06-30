package com.ultron.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A directed edge in the knowledge graph (L4). */
@Entity
@Table(name = "kg_relations")
public class KgRelation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "from_id", nullable = false)
    private UUID fromId;

    @Column(name = "to_id", nullable = false)
    private UUID toId;

    @Column(name = "relation", nullable = false, length = 100)
    private String relation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected KgRelation() {
    }

    public KgRelation(UUID id, UUID fromId, UUID toId, String relation) {
        this.id = id;
        this.fromId = fromId;
        this.toId = toId;
        this.relation = relation;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getFromId() { return fromId; }
    public UUID getToId() { return toId; }
    public String getRelation() { return relation; }
    public Instant getCreatedAt() { return createdAt; }
}
