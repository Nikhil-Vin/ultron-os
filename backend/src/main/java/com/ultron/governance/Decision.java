package com.ultron.governance;

/** Outcome of an {@link ApprovalGate} evaluation. */
public enum Decision {

    /** READ/LOW actions auto-proceed (still audited). */
    AUTO,

    /** HIGH/CRITICAL action explicitly approved by the human operator. */
    APPROVED,

    /** HIGH/CRITICAL action blocked — pending or refused human approval. */
    DENIED
}
