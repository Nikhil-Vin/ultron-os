package com.ultron.governance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The immutable audit trail (L6). Every gated action — approved, denied, or auto —
 * is recorded here with timestamp, risk level, decision, actor and detail.
 *
 * <p>Records are append-only: there is intentionally no update or delete API.
 */
@Service
public class AuditLog {

    private static final Logger log = LoggerFactory.getLogger(AuditLog.class);

    private final AuditEntryRepository repository;

    public AuditLog(AuditEntryRepository repository) {
        this.repository = repository;
    }

    /**
     * Append a record for an evaluated action.
     *
     * @return the persisted entry
     */
    @Transactional
    public AuditEntry record(ProposedAction action, Decision decision) {
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            action.name(),
            action.riskLevel(),
            decision,
            action.actor(),
            action.detail(),
            Instant.now());
        AuditEntry saved = repository.save(entry);
        log.info("AUDIT action={} risk={} decision={} actor={}",
            action.name(), action.riskLevel(), decision, action.actor());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuditEntry> recent() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }
}
