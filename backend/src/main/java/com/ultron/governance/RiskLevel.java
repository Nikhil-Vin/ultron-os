package com.ultron.governance;

/**
 * Section 14 risk classification. Every action Ultron may take is tagged with one level,
 * which determines gating, auditing, and (later) voice-biometric requirements.
 */
public enum RiskLevel {

    /** Query memory, view brief, read email. Runs freely; logged only. */
    READ(false, false),

    /** Save memory, create draft, local notes, paper trade. Logged + notified. */
    LOW(false, false),

    /** Send email, push code, modify calendar. Requires the approval gate. */
    HIGH(true, false),

    /** Live trade, purchase, delete data, phone call, spend. Gate + voice biometric. */
    CRITICAL(true, true);

    private final boolean requiresApproval;
    private final boolean requiresVoiceBiometric;

    RiskLevel(boolean requiresApproval, boolean requiresVoiceBiometric) {
        this.requiresApproval = requiresApproval;
        this.requiresVoiceBiometric = requiresVoiceBiometric;
    }

    /** True for HIGH and CRITICAL — these are blocked until explicit approval. */
    public boolean requiresApproval() {
        return requiresApproval;
    }

    /** True for CRITICAL — voice biometric confirmation required (enforced in Phase 2). */
    public boolean requiresVoiceBiometric() {
        return requiresVoiceBiometric;
    }
}
