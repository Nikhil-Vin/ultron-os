package com.ultron.governance;

import com.ultron.config.UltronProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Voice biometric gate (L6 — Section 14). CRITICAL actions require that the speaker be verified as
 * the owner: the Python voice layer computes a Resemblyzer cosine similarity between the live
 * utterance and the enrolled voiceprint and passes the score here. This gate decides pass/fail
 * against the configured threshold and writes an immutable audit entry for every check.
 *
 * <p>Fail-safe + secure-by-default: if no voiceprint is enrolled, CRITICAL voice actions are
 * BLOCKED (never silently allowed). This sits in front of {@link ApprovalGate} for the voice path.
 */
@Component
public class VoiceIdGate {

    private static final Logger log = LoggerFactory.getLogger(VoiceIdGate.class);

    private final UltronProperties properties;
    private final AuditLog auditLog;

    public VoiceIdGate(UltronProperties properties, AuditLog auditLog) {
        this.properties = properties;
        this.auditLog = auditLog;
    }

    /**
     * Verify a speaker for a proposed action.
     *
     * @param action     the action being attempted
     * @param similarity Resemblyzer cosine similarity in [0,1] from the voice layer (null = no audio)
     * @return the verification result (with the audit id)
     */
    public Result verify(ProposedAction action, Double similarity) {
        UltronProperties.Voice cfg = properties.getVoice();
        double threshold = cfg.getBiometricThreshold();

        boolean required = action.riskLevel().requiresVoiceBiometric();
        boolean passed;
        String reason;

        if (!required) {
            passed = true;
            reason = "not-required";
        } else if (!cfg.isBiometricEnrolled()) {
            passed = false;
            reason = "no-voiceprint-enrolled";
        } else if (similarity == null) {
            passed = false;
            reason = "no-voice-sample";
        } else if (similarity >= threshold) {
            passed = true;
            reason = "match (" + fmt(similarity) + " >= " + fmt(threshold) + ")";
        } else {
            passed = false;
            reason = "below-threshold (" + fmt(similarity) + " < " + fmt(threshold) + ")";
        }

        Decision decision = passed
            ? (required ? Decision.APPROVED : Decision.AUTO)
            : Decision.DENIED;
        AuditEntry entry = auditLog.record(
            new ProposedAction("voiceid." + action.name(), action.riskLevel(),
                "Voice biometric: " + reason, action.actor()),
            decision);

        if (required) {
            log.info("VoiceIdGate action={} required=true passed={} reason={}", action.name(), passed, reason);
        }
        return new Result(passed, reason, entry.getId().toString());
    }

    private static String fmt(double v) {
        return String.format("%.3f", v);
    }

    /**
     * Voice verification outcome.
     *
     * @param passed  true if the speaker is authorised for this action
     * @param reason  human-readable explanation
     * @param auditId id of the audit entry written for this check
     */
    public record Result(boolean passed, String reason, String auditId) {
    }
}
