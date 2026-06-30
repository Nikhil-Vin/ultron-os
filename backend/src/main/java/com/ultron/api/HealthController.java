package com.ultron.api;

import com.ultron.config.UltronProperties;
import com.ultron.connectors.github.GithubConnector;
import com.ultron.intelligence.BrainSelector;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final GithubConnector github;
    private final UltronProperties properties;

    public HealthController(BrainSelector brain, GithubConnector github, UltronProperties properties) {
        this.brain = brain;
        this.github = github;
        this.properties = properties;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("brain", brain.active().name());      // heuristic | ollama
        body.put("github", github.mode());             // fixture | rest
        body.put("autoApprove", properties.isAutoApprove());
        // Note: tokens / credentials are intentionally never included in this payload.
        return body;
    }
}
