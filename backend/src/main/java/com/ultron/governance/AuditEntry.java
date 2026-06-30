package com.ultron.governance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit record (L6 — The Conscience). One row per gated action attempt.
 * Maps to the {@code audit_log} table (created by Flyway in prod, by Hibernate in tests).
 */
@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "action", nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private Decision decision;

    @Column(name = "actor", nullable = false)
    private String actor;

    @Column(name = "detail")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEntry() {
        // for JPA
    }

    public AuditEntry(UUID id, String action, RiskLevel riskLevel, Decision decision,
                      String actor, String detail, Instant createdAt) {
        this.id = id;
        this.action = action;
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.actor = actor;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getActor() {
        return actor;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
