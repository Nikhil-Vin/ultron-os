package com.ultron.intelligence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Chooses the active {@link Brain} at call time (L3). Prefers {@link OllamaBrain} when it is
 * enabled and reachable; otherwise degrades to the always-available {@link HeuristicBrain}.
 *
 * <p>This is the concrete expression of the "fail-safe degradation" principle: there is always
 * a working brain, so a missing Ollama server never breaks the system.
 */
@Component
public class BrainSelector {

    private static final Logger log = LoggerFactory.getLogger(BrainSelector.class);

    private final OllamaBrain ollamaBrain;
    private final HeuristicBrain heuristicBrain;

    public BrainSelector(OllamaBrain ollamaBrain, HeuristicBrain heuristicBrain) {
        this.ollamaBrain = ollamaBrain;
        this.heuristicBrain = heuristicBrain;
    }

    /** The brain that will actually serve right now. */
    public Brain active() {
        if (ollamaBrain.isAvailable()) {
            return ollamaBrain;
        }
        return heuristicBrain;
    }

    /** Convenience: reason using the active brain. */
    public String think(String prompt) {
        Brain brain = active();
        log.debug("Reasoning via brain={}", brain.name());
        return brain.think(prompt);
    }
}
