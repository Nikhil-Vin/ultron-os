package com.ultron.intelligence;

import com.ultron.config.UltronProperties;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Local LLM brain (L3) backed by an Ollama server over plain REST — no cloud, no paid keys.
 *
 * <p>It is enabled only when {@code ultron.brain.ollama-enabled=true} AND the server responds.
 * Availability is probed with short timeouts so the system never blocks when Ollama is absent;
 * the {@link com.ultron.intelligence.BrainSelector} then degrades to {@link HeuristicBrain}.
 *
 * <p>Spring AI's {@code ChatClient} replaces this hand-rolled client in Phase 1; the {@link Brain}
 * abstraction keeps that swap to a single layer.
 */
@Component
public class OllamaBrain implements Brain {

    private static final Logger log = LoggerFactory.getLogger(OllamaBrain.class);

    private final UltronProperties.Brain config;
    private final RestClient client;

    public OllamaBrain(UltronProperties properties) {
        this.config = properties.getBrain();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofMillis(800))
            .withReadTimeout(Duration.ofSeconds(30));
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
        this.client = RestClient.builder()
            .requestFactory(factory)
            .baseUrl(config.getOllamaBaseUrl())
            .build();
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public boolean isAvailable() {
        if (!config.isOllamaEnabled()) {
            return false;
        }
        try {
            client.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException ex) {
            log.debug("Ollama not reachable at {}: {}", config.getOllamaBaseUrl(), ex.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String think(String prompt) {
        Map<String, Object> body = Map.of(
            "model", config.getOllamaModel(),
            "prompt", prompt == null ? "" : prompt,
            "stream", false);
        Map<String, Object> response = client.post()
            .uri("/api/generate")
            .body(body)
            .retrieve()
            .body(Map.class);
        Object text = response == null ? null : response.get("response");
        return text == null ? "" : text.toString().strip();
    }
}
