package com.ultron.intelligence.embedding;

import com.ultron.config.UltronProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Local embedding model (L3) backed by an Ollama server over plain REST — no cloud, no paid keys.
 *
 * <p>Enabled only when {@code ultron.brain.ollama-enabled=true} AND the server responds. Probing
 * uses short timeouts so the system never blocks when Ollama is absent; {@link EmbedderSelector}
 * then degrades to {@link HeuristicEmbedder}.
 */
@Component
public class OllamaEmbedder implements Embedder {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbedder.class);

    private final UltronProperties.Brain config;
    private final RestClient client;

    /** Latches true once the server reports it cannot embed, so we stop trying (honest health). */
    private volatile boolean embeddingsUnsupported = false;

    public OllamaEmbedder(UltronProperties properties) {
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

    /** The dedicated embedding model in use (reported by health). */
    public String model() {
        return config.getOllamaEmbeddingModel();
    }

    @Override
    public boolean isAvailable() {
        if (!config.isOllamaEnabled() || embeddingsUnsupported) {
            return false;
        }
        try {
            client.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException ex) {
            log.debug("Ollama embeddings not reachable at {}: {}", config.getOllamaBaseUrl(), ex.getMessage());
            return false;
        }
    }

    /** Ollama embedding dimension depends on the model; reported as 0 (unknown until first call). */
    @Override
    public int dimension() {
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }
        Map<String, Object> body = Map.of(
            "model", config.getOllamaEmbeddingModel(),
            "prompt", text);
        try {
            Map<String, Object> response = client.post()
                .uri("/api/embeddings")
                .body(body)
                .retrieve()
                .body(Map.class);
            Object raw = response == null ? null : response.get("embedding");
            if (!(raw instanceof List<?> list) || list.isEmpty()) {
                return new float[0];
            }
            float[] vec = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object n = list.get(i);
                vec[i] = (n instanceof Number num) ? num.floatValue() : 0f;
            }
            return Vectors.l2normalize(vec);
        } catch (RuntimeException ex) {
            // Fail-safe: the model/server can't embed → latch unavailable and return empty so
            // EmbedderSelector degrades to the heuristic embedder (Section 4 fail-safe degradation).
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            if (msg.contains("does not support embeddings")) {
                embeddingsUnsupported = true;
                log.warn("Ollama model '{}' does not support embeddings; degrading to heuristic embedder",
                    config.getOllamaEmbeddingModel());
            } else {
                log.warn("Ollama embedding call failed ({}); degrading to heuristic embedder", msg);
            }
            return new float[0];
        }
    }
}
