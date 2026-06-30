package com.ultron.governance;

import java.util.Objects;

/**
 * An action Ultron proposes to take, presented to the {@link ApprovalGate}.
 *
 * @param name       short machine-friendly action name, e.g. {@code memory.save}
 * @param riskLevel  the risk classification (Section 14)
 * @param detail     human-readable description of what will happen
 * @param actor      who/what initiated the action (worker, user, scheduler)
 */
public record ProposedAction(String name, RiskLevel riskLevel, String detail, String actor) {

    public ProposedAction {
        Objects.requireNonNull(name, "action name is required");
        Objects.requireNonNull(riskLevel, "riskLevel is required");
        if (actor == null || actor.isBlank()) {
            actor = "system";
        }
    }

    public static ProposedAction of(String name, RiskLevel riskLevel, String detail) {
        return new ProposedAction(name, riskLevel, detail, "system");
    }
}
