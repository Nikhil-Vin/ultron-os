package com.ultron.api;

import com.ultron.config.UltronProperties;
import com.ultron.connectors.github.GithubConnector;
import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.workers.WorkerRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/health} — liveness + the active capability profile, with no secrets exposed.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final BrainSelector brain;
    private final EmbedderSelector embedder;
    private final GithubConnector github;
    private final WorkerRegistry workers;
    private final UltronProperties properties;

    public HealthController(BrainSelector brain, EmbedderSelector embedder, GithubConnector github,
                            WorkerRegistry workers, UltronProperties properties) {
        this.brain = brain;
        this.embedder = embedder;
        this.github = github;
        this.workers = workers;
        this.properties = properties;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        BrainSelector.Status brainStatus = brain.status();
        body.put("brain", brainStatus.active());           // heuristic | ollama
        body.put("brainModel", brainStatus.model());       // model name | n/a
        body.put("llmActive", brainStatus.llmActive());    // true when real LLM is serving
        EmbedderSelector.Status embStatus = embedder.status();
        body.put("embedder", embStatus.active());          // heuristic | ollama
        body.put("embedderModel", embStatus.model());      // nomic-embed-text | hashing-512
        body.put("github", github.mode());                 // fixture | rest
        body.put("workers", new TreeSet<>(workers.names()));
        body.put("autoApprove", properties.isAutoApprove());
        // Note: tokens / credentials are intentionally never included in this payload.
        return body;
    }
}
