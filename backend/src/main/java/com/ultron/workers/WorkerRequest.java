package com.ultron.workers;

import java.util.Map;

/**
 * Input to a {@link Worker}.
 *
 * @param kind   the operation requested (worker-specific, e.g. {@code brief})
 * @param params free-form parameters for the operation
 */
public record WorkerRequest(String kind, Map<String, Object> params) {

    public WorkerRequest {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("worker request kind is required");
        }
        params = (params == null) ? Map.of() : Map.copyOf(params);
    }

    public static WorkerRequest of(String kind) {
        return new WorkerRequest(kind, Map.of());
    }
}
