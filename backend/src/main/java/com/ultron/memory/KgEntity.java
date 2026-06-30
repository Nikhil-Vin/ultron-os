package com.ultron.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** A node in the knowledge graph (L4 — Section 9.1). */
@Entity
@Table(name = "kg_entities", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "type"}))
public class KgEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected KgEntity() {
    }

    public KgEntity(UUID id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}
