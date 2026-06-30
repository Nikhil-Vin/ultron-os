package com.ultron.intelligence;

import com.ultron.config.UltronProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
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
 * <p>Enabled only when {@code ultron.brain.ollama-enabled=true} AND the server responds. The
 * availability probe is cached for a short TTL so reasoning never re-probes on every call, and a
 * failed generation degrades safely (returns blank → {@link BrainSelector} keeps the heuristic
 * fallback usable). Ultron's persona is injected as a system prompt; generation options
 * (temperature, keep-alive) come from {@link UltronProperties.Brain}. Zero secrets are sent.
 */
@Component
public class OllamaBrain implements Brain {

    private static final Logger log = LoggerFactory.getLogger(OllamaBrain.class);

    private final UltronProperties.Brain config;
    private final RestClient client;

    // Cached availability probe: value (0=unknown,1=up,2=down) + expiry timestamp.
    private final AtomicLong cachedProbeExpiry = new AtomicLong(0);
    private volatile boolean cachedAvailable = false;

    public OllamaBrain(UltronProperties properties) {
        this.config = properties.getBrain();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofMillis(800))
            .withReadTimeout(Duration.ofSeconds(60));
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

    /** The configured model this brain reasons with (reported by health). */
    public String model() {
        return config.getOllamaModel();
    }

    @Override
    public boolean isAvailable() {
        if (!config.isOllamaEnabled()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < cachedProbeExpiry.get()) {
            return cachedAvailable;
        }
        boolean up;
        try {
            client.get().uri("/api/tags").retrieve().toBodilessEntity();
            up = true;
        } catch (RuntimeException ex) {
            log.debug("Ollama not reachable at {}: {}", config.getOllamaBaseUrl(), ex.getMessage());
            up = false;
        }
        cachedAvailable = up;
        cachedProbeExpiry.set(now + Math.max(0, config.getAvailabilityCacheMillis()));
        return up;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String think(String prompt) {
        String userPrompt = prompt == null ? "" : prompt;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getOllamaModel());
        body.put("prompt", userPrompt);
        body.put("system", config.getSystemPrompt());
        body.put("stream", false);
        body.put("keep_alive", config.getKeepAlive());
        body.put("options", Map.of("temperature", config.getTemperature()));

        try {
            Map<String, Object> response = client.post()
                .uri("/api/generate")
                .body(body)
                .retrieve()
                .body(Map.class);
            Object text = response == null ? null : response.get("response");
            String answer = text == null ? "" : text.toString().strip();
            if (answer.isEmpty()) {
                log.warn("Ollama returned an empty response for model={}", config.getOllamaModel());
            }
            return answer;
        } catch (RuntimeException ex) {
            // Fail-safe: invalidate the cached probe and return blank so the selector degrades.
            cachedProbeExpiry.set(0);
            log.warn("Ollama generation failed ({}); degrading to fallback", ex.getMessage());
            return "";
        }
    }

    /**
     * Stream a reasoned reply token-by-token (real streaming via Ollama {@code stream:true}). Each
     * token is delivered to {@code onToken} as it arrives; the full text is returned at the end.
     * Returns blank on failure so the caller (VoiceController) can fall back to the heuristic.
     *
     * @param prompt  the user prompt
     * @param onToken consumer invoked with each token fragment as it streams
     * @return the full concatenated response
     */
    @SuppressWarnings("unchecked")
    public String streamThink(String prompt, Consumer<String> onToken) {
        String userPrompt = prompt == null ? "" : prompt;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getOllamaModel());
        body.put("prompt", userPrompt);
        body.put("system", config.getSystemPrompt());
        body.put("stream", true);
        body.put("keep_alive", config.getKeepAlive());
        body.put("options", Map.of("temperature", config.getTemperature()));

        ObjectMapper mapper = new ObjectMapper();
        StringBuilder full = new StringBuilder();
        try {
            String json = mapper.writeValueAsString(body);
            HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(800))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getOllamaBaseUrl() + "/api/generate"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<java.io.InputStream> response =
                http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(response.body(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    Map<String, Object> chunk = mapper.readValue(line, Map.class);
                    Object token = chunk.get("response");
                    if (token != null) {
                        String t = token.toString();
                        full.append(t);
                        if (!t.isEmpty()) {
                            onToken.accept(t);
                        }
                    }
                    if (Boolean.TRUE.equals(chunk.get("done"))) {
                        break;
                    }
                }
            }
            return full.toString().strip();
        } catch (Exception ex) {
            cachedProbeExpiry.set(0);
            log.warn("Ollama streaming failed ({}); degrading to fallback", ex.getMessage());
            return full.toString().strip();
        }
    }
}
