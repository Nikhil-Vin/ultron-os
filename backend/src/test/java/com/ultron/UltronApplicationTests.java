package com.ultron;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.intelligence.BrainSelector;
import com.ultron.workers.WorkerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: the entire Phase 0 bean graph wires and boots on H2 (offline). Confirms there are
 * no ambiguous-bean or missing-dependency issues across all 6 layers.
 */
@SpringBootTest
class UltronApplicationTests {

    @Autowired
    private WorkerRegistry workerRegistry;

    @Autowired
    private BrainSelector brainSelector;

    @Test
    void contextLoadsAndSentinelIsRegistered() {
        assertThat(workerRegistry.has("sentinel")).isTrue();
    }

    @Test
    void offlineBrainIsAvailableByDefault() {
        // Ollama disabled in test profile → active brain is the heuristic fallback.
        assertThat(brainSelector.active().name()).isEqualTo("heuristic");
    }
}
