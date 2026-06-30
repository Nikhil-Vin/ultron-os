package com.ultron.governance;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Lightweight in-memory rate limiter (L6). Protects the boundary from runaway loops or abusive
 * callers without any external dependency. Uses a per-key sliding window of request timestamps.
 *
 * <p>Local-first and stateless across restarts by design (a solo-operator system); swap in a
 * shared store only if Ultron ever runs multi-node.
 */
@Component
public class RateLimiter {

    /** Default budget: 60 permits per 60s window per key. */
    private static final int DEFAULT_LIMIT = 60;
    private static final Duration DEFAULT_WINDOW = Duration.ofSeconds(60);

    private final int limit;
    private final long windowMillis;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter() {
        this(DEFAULT_LIMIT, DEFAULT_WINDOW);
    }

    public RateLimiter(int limit, Duration window) {
        this.limit = limit;
        this.windowMillis = window.toMillis();
    }

    /** Try to consume one permit for {@code key}. Returns true if allowed, false if throttled. */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, System.currentTimeMillis());
    }

    /** Testable variant with an injected clock value. */
    boolean tryAcquire(String key, long nowMillis) {
        Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            long cutoff = nowMillis - windowMillis;
            while (!window.isEmpty() && window.peekFirst() < cutoff) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                return false;
            }
            window.addLast(nowMillis);
            return true;
        }
    }

    /** Current number of permits used for a key within the window (best-effort). */
    public int currentUsage(String key) {
        Deque<Long> window = hits.get(key);
        if (window == null) {
            return 0;
        }
        synchronized (window) {
            long cutoff = System.currentTimeMillis() - windowMillis;
            while (!window.isEmpty() && window.peekFirst() < cutoff) {
                window.pollFirst();
            }
            return window.size();
        }
    }

    public int limit() {
        return limit;
    }
}
