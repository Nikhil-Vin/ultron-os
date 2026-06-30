package com.ultron.governance;

import com.ultron.config.UltronProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The human-in-the-loop approval gate (L6 — The Conscience).
 *
 * <p>Behaviour (Section 14):
 * <ul>
 *   <li>{@code READ} / {@code LOW} → proceed automatically ({@link Decision#AUTO}), always audited.</li>
 *   <li>{@code HIGH} / {@code CRITICAL} → blocked ({@link Decision#DENIED}) unless an explicit human
 *       approval is supplied. With {@code ultron.auto-approve=false} (the hardcoded default) these
 *       never proceed without a deliberate human "go".</li>
 * </ul>
 *
 * <p>Every evaluation writes an {@link AuditLog} entry, so there is no path to action without a record.
 */
@Component
public class ApprovalGate {

    private static final Logger log = LoggerFactory.getLogger(ApprovalGate.class);

    private final UltronProperties properties;
    private final AuditLog auditLog;

    public ApprovalGate(UltronProperties properties, AuditLog auditLog) {
        this.properties = properties;
        this.auditLog = auditLog;
    }

    /**
     * Evaluate an action with no explicit human approval (the common path).
     */
    public GateResult evaluate(ProposedAction action) {
        return evaluate(action, false);
    }

    /**
     * Evaluate an action, optionally carrying an explicit human approval signal.
     *
     * @param action       the proposed action
     * @param humanApproved true if the operator has explicitly approved this specific action
     */
    public GateResult evaluate(ProposedAction action, boolean humanApproved) {
        Decision decision = decide(action, humanApproved);
        AuditEntry entry = auditLog.record(action, decision);
        boolean allowed = decision != Decision.DENIED;
        if (!allowed) {
            log.warn("Gate BLOCKED action={} risk={} (auto-approve={})",
                action.name(), action.riskLevel(), properties.isAutoApprove());
        }
        return new GateResult(allowed, decision, entry.getId().toString());
    }

    private Decision decide(ProposedAction action, boolean humanApproved) {
        if (!action.riskLevel().requiresApproval()) {
            return Decision.AUTO;
        }
        if (humanApproved || properties.isAutoApprove()) {
            return Decision.APPROVED;
        }
        return Decision.DENIED;
    }

    /**
     * Result of a gate evaluation.
     *
     * @param allowed   whether the caller may proceed with the action
     * @param decision  the recorded decision
     * @param auditId   id of the audit entry written for this evaluation
     */
    public record GateResult(boolean allowed, Decision decision, String auditId) {
    }
}
