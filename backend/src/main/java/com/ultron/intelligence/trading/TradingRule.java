package com.ultron.intelligence.trading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A personal trading rule, often sourced from an ingested skill (book/PDF) — Section 7. */
@Entity
@Table(name = "trading_rules")
public class TradingRule {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "rule_text", nullable = false)
    private String ruleText;

    @Column(name = "source_skill_tag", length = 100)
    private String sourceSkillTag;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected TradingRule() {
    }

    public TradingRule(UUID id, String ruleName, String ruleText, String sourceSkillTag, boolean active) {
        this.id = id;
        this.ruleName = ruleName;
        this.ruleText = ruleText;
        this.sourceSkillTag = sourceSkillTag;
        this.active = active;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getRuleName() { return ruleName; }
    public String getRuleText() { return ruleText; }
    public String getSourceSkillTag() { return sourceSkillTag; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setActive(boolean active) { this.active = active; }
}
