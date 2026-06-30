package com.ultron.intelligence;

import org.springframework.stereotype.Component;

/**
 * The offline-first fallback brain (L3). Always available, no model, no network — it produces
 * deterministic, useful summaries so the system stays functional with zero dependencies.
 *
 * <p>This is the fail-safe degradation target: if Ollama and all cloud models are absent,
 * Ultron still runs on the {@code HeuristicBrain}.
 */
@Component
public class HeuristicBrain implements Brain {

    @Override
    public String name() {
        return "heuristic";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String think(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "I have nothing to reason about yet.";
        }
        String trimmed = prompt.strip();
        String firstLine = trimmed.lines().findFirst().orElse(trimmed);
        int words = trimmed.split("\\s+").length;
        return "[heuristic] Noted (%d words). Focus: %s".formatted(
            words, firstLine.length() > 160 ? firstLine.substring(0, 157) + "..." : firstLine);
    }
}
