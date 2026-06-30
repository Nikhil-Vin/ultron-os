package com.ultron.intelligence.bridge;

import com.ultron.config.PythonBridgeConfig;
import java.time.Duration;
import java.util.HashMap;
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
 * Thin client for the optional Python {@code ai-layer} (FastAPI, L3/L7).
 *
 * <p>Opt-in by design: it does nothing unless {@code ultron.python-bridge.enabled=true} and a base
 * URL is set ({@link PythonBridgeConfig#isUsable()}). When the bridge is down or disabled, callers
 * fall back to the in-JVM heuristic paths — the same fail-safe degradation the rest of L3 follows
 * (Section 4). The shared {@code X-API-Key} is sent only when configured and is never logged.
 */
@Component
public class PythonBridgeClient {

    private static final Logger log = LoggerFactory.getLogger(PythonBridgeClient.class);

    private final PythonBridgeConfig config;
    private final RestClient client;

    public PythonBridgeClient(PythonBridgeConfig config) {
        this.config = config;
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofMillis(800))
            .withReadTimeout(Duration.ofSeconds(30));
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
        this.client = RestClient.builder()
            .requestFactory(factory)
            .baseUrl(config.getBaseUrl())
            .build();
    }

    /** True when the bridge is enabled, configured, and currently reachable. */
    public boolean isAvailable() {
        if (!config.isUsable()) {
            return false;
        }
        try {
            client.get().uri("/health").headers(this::auth).retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException ex) {
            log.debug("ai-layer bridge not reachable at {}: {}", config.getBaseUrl(), ex.getMessage());
            return false;
        }
    }

    /**
     * Embed text via the ai-layer. Returns an empty array when the bridge is unavailable or errors,
     * so callers degrade to the local embedder rather than failing.
     */
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        if (!config.isUsable() || text == null || text.isBlank()) {
            return new float[0];
        }
        try {
            Map<String, Object> response = client.post()
                .uri("/embed")
                .headers(this::auth)
                .body(Map.of("text", text))
                .retrieve()
                .body(Map.class);
            Object raw = response == null ? null : response.get("vector");
            if (!(raw instanceof List<?> list) || list.isEmpty()) {
                return new float[0];
            }
            float[] vec = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object n = list.get(i);
                vec[i] = (n instanceof Number num) ? num.floatValue() : 0f;
            }
            return vec;
        } catch (RuntimeException ex) {
            log.warn("ai-layer embed failed ({}); caller should fall back to local embedder", ex.getMessage());
            return new float[0];
        }
    }

    /**
     * Delegate skill intake to the ai-layer for rich formats (PDF, URL, YouTube).
     * Returns a map with keys {@code format}, {@code chunk_count}, etc. or empty map on failure.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> intakeSkill(String name, String text, String url, String contentType) {
        if (!config.isUsable()) {
            return Map.of();
        }
        try {
            var body = new HashMap<String, Object>();
            body.put("name", name);
            if (text != null && !text.isBlank()) body.put("text", text);
            if (url != null && !url.isBlank()) body.put("url", url);
            if (contentType != null && !contentType.isBlank()) body.put("content_type", contentType);
            body.put("dedup", true);

            Map<String, Object> response = client.post()
                .uri("/skills/intake")
                .headers(this::auth)
                .body(body)
                .retrieve()
                .body(Map.class);
            return response != null ? response : Map.of();
        } catch (RuntimeException ex) {
            log.warn("ai-layer skill intake failed ({}); falling back to local text intake", ex.getMessage());
            return Map.of();
        }
    }

    private void auth(org.springframework.http.HttpHeaders headers) {
        String key = config.getApiKey();
        if (key != null && !key.isBlank()) {
            headers.add("X-API-Key", key);
        }
    }
}
