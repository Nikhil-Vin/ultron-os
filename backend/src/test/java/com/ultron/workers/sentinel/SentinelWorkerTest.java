package com.ultron.workers.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultron.config.UltronProperties;
import com.ultron.connectors.github.FixtureGithubConnector;
import com.ultron.connectors.github.GithubConnector;
import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.HeuristicBrain;
import com.ultron.intelligence.OllamaBrain;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test — no Spring context, no network. Ollama is disabled so {@link BrainSelector}
 * resolves to the offline {@link HeuristicBrain} instantly (no socket attempt).
 */
class SentinelWorkerTest {

    private SentinelWorker newSentinel() {
        GithubConnector github = new FixtureGithubConnector(new ObjectMapper());

        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaEnabled(false); // force heuristic, no network probe
        BrainSelector brain = new BrainSelector(new OllamaBrain(props), new HeuristicBrain());

        return new SentinelWorker(github, brain);
    }

    @Test
    void rendersBriefFromFixtureData() {
        SentinelWorker sentinel = newSentinel();

        WorkerResult result = sentinel.handle(WorkerRequest.of("brief"));

        assertThat(result.success()).isTrue();
        assertThat(result.summary())
            .contains("Overnight brief for Nikhil-Vin")
            .contains("ultron-os")
            .contains("#42")
            .contains("CHANGES_REQUESTED")
            .contains("pgvector index tuning")
            .contains("Insight:");
        assertThat(result.detail()).isEqualTo("github-mode=fixture");
    }

    @Test
    void briefAlwaysIncludesAnInsightLineFromOfflineBrain() {
        SentinelWorker sentinel = newSentinel();

        String brief = sentinel.render(new FixtureGithubConnector(new ObjectMapper()).snapshot());

        assertThat(brief).contains("[heuristic]");
    }
}
