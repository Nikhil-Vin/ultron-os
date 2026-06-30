package com.ultron.kernel;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Work-mode manager (L1 — Nervous System). Tracks Ultron's active behavioural mode (Section 9.8)
 * and exposes a per-mode {@link Behavior} profile that the voice/HUD/psychology layers consult to
 * shape verbosity, proactivity, and spoken delivery.
 *
 * <p>State is in-memory and process-local (solo-operator). Mode can be set by the kernel, the
 * scheduler (e.g. MORNING_BRIEF at 07:00), or by a voice command routed through the VoiceController.
 */
@Component
public class WorkModeManager {

    private static final Logger log = LoggerFactory.getLogger(WorkModeManager.class);

    /** Behavioural modes from Section 9.8. */
    public enum WorkMode {
        TRADING(true, false, 0.15),       // markets-focused, clipped, psychology checks active
        DEEP_WORK(true, false, 0.10),     // ultra-terse, only CRITICAL interrupts
        PLANNING(false, true, 0.80),      // verbose, retrospective, surfaces deferred work
        RESEARCH(false, true, 0.75),      // scholar mode, save everything
        CASUAL(false, true, 0.60),        // relaxed, creative
        MORNING_BRIEF(false, true, 0.65); // sentinel brief style

        private final boolean terse;
        private final boolean suggestionsEnabled;
        private final double verbosity; // 0..1

        WorkMode(boolean terse, boolean suggestionsEnabled, double verbosity) {
            this.terse = terse;
            this.suggestionsEnabled = suggestionsEnabled;
            this.verbosity = verbosity;
        }

        public Behavior behavior() {
            return new Behavior(this, terse, suggestionsEnabled, verbosity);
        }
    }

    private final AtomicReference<WorkMode> current = new AtomicReference<>(WorkMode.CASUAL);

    public WorkMode current() {
        return current.get();
    }

    public Behavior behavior() {
        return current.get().behavior();
    }

    public WorkMode setMode(WorkMode mode) {
        WorkMode previous = current.getAndSet(mode);
        log.info("Work mode {} -> {}", previous, mode);
        return mode;
    }

    /**
     * Resolve a mode from a free-form phrase (e.g. a voice command "trading mode", "let's focus").
     * Returns null when nothing matches so the caller can keep the current mode.
     */
    public WorkMode fromPhrase(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return null;
        }
        String t = phrase.toLowerCase();
        if (t.contains("trading")) return WorkMode.TRADING;
        if (t.contains("deep work") || t.contains("focus")) return WorkMode.DEEP_WORK;
        if (t.contains("planning") || t.contains("plan mode") || t.contains("review")) return WorkMode.PLANNING;
        if (t.contains("research")) return WorkMode.RESEARCH;
        if (t.contains("casual")) return WorkMode.CASUAL;
        if (t.contains("morning") || t.contains("brief")) return WorkMode.MORNING_BRIEF;
        return null;
    }

    /**
     * Per-mode behaviour profile.
     *
     * @param mode               the active mode
     * @param terse              true → keep spoken output clipped and fast
     * @param suggestionsEnabled true → proactive nudges/suggestions allowed
     * @param verbosity          0 (minimal) .. 1 (expansive)
     */
    public record Behavior(WorkMode mode, boolean terse, boolean suggestionsEnabled, double verbosity) {
    }
}
