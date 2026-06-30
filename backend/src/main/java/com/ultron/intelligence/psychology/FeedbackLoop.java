package com.ultron.intelligence.psychology;

import com.ultron.intelligence.psychology.IntentClassifier.Intent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Online feedback loop (L7 — The Mind). Records thumbs-up / thumbs-down on the agent's handling of
 * each {@link Intent} and maintains a running confidence weight per intent. Phase 1 is a simple
 * in-memory exponential update (the offline analogue of the Python River online-learning model);
 * it lets downstream components prefer intents the owner has reinforced.
 */
@Component
public class FeedbackLoop {

    /** EMA smoothing factor for each new signal. */
    private static final double ALPHA = 0.2;
    private static final double START = 0.5;

    private final Map<Intent, Double> weights = new ConcurrentHashMap<>();

    /** Record feedback for an intent: {@code positive=true} reinforces, {@code false} penalises. */
    public void record(Intent intent, boolean positive) {
        if (intent == null) {
            return;
        }
        double target = positive ? 1.0 : 0.0;
        weights.merge(intent, ALPHA * target + (1 - ALPHA) * START,
            (oldVal, ignored) -> ALPHA * target + (1 - ALPHA) * oldVal);
    }

    /** Current confidence weight in [0,1] for an intent (defaults to the neutral start). */
    public double weight(Intent intent) {
        return weights.getOrDefault(intent, START);
    }

    /** Snapshot of all learned weights. */
    public Map<Intent, Double> snapshot() {
        return Map.copyOf(weights);
    }
}
