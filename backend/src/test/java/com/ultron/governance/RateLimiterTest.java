package com.ultron.governance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Pure unit test for the sliding-window rate limiter. */
class RateLimiterTest {

    @Test
    void allowsUpToLimitThenThrottles() {
        RateLimiter limiter = new RateLimiter(3, Duration.ofSeconds(60));
        long t = 1_000L;

        assertThat(limiter.tryAcquire("k", t)).isTrue();
        assertThat(limiter.tryAcquire("k", t)).isTrue();
        assertThat(limiter.tryAcquire("k", t)).isTrue();
        assertThat(limiter.tryAcquire("k", t)).isFalse(); // 4th in window → throttled
    }

    @Test
    void windowSlidesAndPermitsRecover() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(10));
        assertThat(limiter.tryAcquire("k", 0L)).isTrue();
        assertThat(limiter.tryAcquire("k", 1_000L)).isTrue();
        assertThat(limiter.tryAcquire("k", 2_000L)).isFalse();
        // Advance past the 10s window of the earliest hits.
        assertThat(limiter.tryAcquire("k", 12_000L)).isTrue();
    }

    @Test
    void keysAreIsolated() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60));
        assertThat(limiter.tryAcquire("a", 0L)).isTrue();
        assertThat(limiter.tryAcquire("b", 0L)).isTrue();
        assertThat(limiter.tryAcquire("a", 0L)).isFalse();
    }
}
