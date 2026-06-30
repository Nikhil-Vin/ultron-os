package com.ultron;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.workers.WorkerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: the entire bean graph wires and boots on H2 (offline). Confirms there are no
 * ambiguous-bean or missing-dependency issues across all layers, now including the Phase 1
 * embedder, RAG layer, skill intake and the full worker staff.
 */
@SpringBootTest
class UltronApplicationTests {

    @Autowired
    private WorkerRegistry workerRegistry;

    @Autowired
    private BrainSelector brainSelector;

    @Autowired
    private EmbedderSelector embedderSelector;

    @Test
    void contextLoadsAndAllWorkersAreRegistered() {
        assertThat(workerRegistry.names())
            .contains("sentinel", "archivist", "planner", "scholar", "trader");
    }

    @Test
    void offlineBrainAndEmbedderAreAvailableByDefault() {
        // Ollama disabled in test profile → active brain + embedder are the heuristic fallbacks.
        assertThat(brainSelector.active().name()).isEqualTo("heuristic");
        assertThat(embedderSelector.active().name()).isEqualTo("heuristic");
    }
}
