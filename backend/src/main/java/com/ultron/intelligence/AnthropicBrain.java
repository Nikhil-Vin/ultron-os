package com.ultron.intelligence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultron.config.UltronProperties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Anthropic Claude brain (L3) — opt-in via ANTHROPIC_API_KEY. Messages API (x-api-key +
 * anthropic-version). Supports SSE streaming (content_block_delta events).
 */
@Component
public class AnthropicBrain implements Brain {

    private static final Logger log = LoggerFactory.getLogger(AnthropicBrain.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE = "https://api.anthropic.com/v1";

    private final UltronProperties.Brain config;
    private final RestClient client;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public AnthropicBrain(UltronProperties properties) {
        this.config = properties.getBrain();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(3))
            .withReadTimeout(Duration.ofSeconds(60));
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
        this.client = RestClient.builder().requestFactory(factory).baseUrl(BASE).build();
    }

    @Override
    public String name() {
        return "anthropic";
    }

    public String model() {
        return config.getAnthropicModel();
    }

    @Override
    public boolean isAvailable() {
        return config.getAnthropicApiKey() != null && !config.getAnthropicApiKey().isBlank();
    }

    private Map<String, Object> body(String prompt, boolean stream) {
        return Map.of(
            "model", config.getAnthropicModel(),
            "max_tokens", 1024,
            "temperature", config.getTemperature(),
            "stream", stream,
            "system", config.getSystemPrompt(),
            "messages", List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String think(String prompt) {
        if (!isAvailable()) {
            return "";
        }
        try {
            Map<String, Object> res = client.post()
                .uri("/messages")
                .header("x-api-key", config.getAnthropicApiKey())
                .header("anthropic-version", "2023-06-01")
                .body(body(prompt, false))
                .retrieve().body(Map.class);
            List<Map<String, Object>> content = res == null ? null : (List<Map<String, Object>>) res.get("content");
            if (content == null || content.isEmpty()) {
                return "";
            }
            Object text = content.get(0).get("text");
            return text == null ? "" : text.toString().strip();
        } catch (RuntimeException ex) {
            log.warn("Anthropic call failed ({}); degrading", ex.getMessage());
            return "";
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String streamThink(String prompt, Consumer<String> onToken) {
        if (!isAvailable()) {
            return "";
        }
        StringBuilder full = new StringBuilder();
        try {
            String json = MAPPER.writeValueAsString(body(prompt, true));
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/messages"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.getAnthropicApiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<java.io.InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) continue;
                    Map<String, Object> ev = MAPPER.readValue(data, Map.class);
                    if (!"content_block_delta".equals(ev.get("type"))) continue;
                    Map<String, Object> delta = (Map<String, Object>) ev.get("delta");
                    Object tok = delta == null ? null : delta.get("text");
                    if (tok != null && !tok.toString().isEmpty()) {
                        full.append(tok);
                        onToken.accept(tok.toString());
                    }
                }
            }
            if (full.length() > 0) {
                return full.toString().strip();
            }
        } catch (Exception ex) {
            log.warn("Anthropic streaming failed ({}); falling back", ex.getMessage());
        }
        String a = think(prompt);
        if (a != null && !a.isBlank() && full.length() == 0) {
            onToken.accept(a);
        }
        return a;
    }
}
