package com.ultron.intelligence.psychology;

import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Priority scoring (L7 — The Mind). Produces a 0..1 importance score for an item so the Planner
 * and proactive layers can order work. Offline + deterministic in Phase 1 (keyword + signal
 * heuristics); swappable for the XGBoost model in the Python layer later.
 */
@Component
public class PriorityScorer {

    private static final String[] URGENT = {"urgent", "asap", "immediately", "now", "critical", "emergency", "today", "deadline"};
    private static final String[] HIGH = {"important", "must", "blocker", "blocked", "overdue", "high priority"};
    private static final String[] LOW = {"someday", "maybe", "whenever", "low priority", "nice to have", "fyi"};

    /** Score in [0,1]. Higher = more urgent/important. */
    public double score(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        String t = text.toLowerCase(Locale.ROOT);
        double score = 0.3; // neutral baseline

        for (String w : URGENT) {
            if (t.contains(w)) {
                score += 0.25;
            }
        }
        for (String w : HIGH) {
            if (t.contains(w)) {
                score += 0.15;
            }
        }
        for (String w : LOW) {
            if (t.contains(w)) {
                score -= 0.2;
            }
        }
        if (t.contains("!")) {
            score += 0.05;
        }
        return clamp(score);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
