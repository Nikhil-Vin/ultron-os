package com.ultron.intelligence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultron.config.UltronProperties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
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
 * Google Gemini cloud brain (L3) — opt-in via GOOGLE_API_KEY. Uses generateContent, and
 * streamGenerateContent (SSE) for real-time token streaming.
 */
@Component
public class GeminiBrain implements Brain {

    private static final Logger log = LoggerFactory.getLogger(GeminiBrain.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";

    private final UltronProperties.Brain config;
    private final RestClient client;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public GeminiBrain(UltronProperties properties) {
        this.config = properties.getBrain();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(3))
            .withReadTimeout(Duration.ofSeconds(60));
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
        this.client = RestClient.builder().requestFactory(factory).baseUrl(BASE).build();
    }

    @Override
    public String name() {
        return "gemini";
    }

    public String model() {
        return config.getGeminiModel();
    }

    @Override
    public boolean isAvailable() {
        return config.getGeminiApiKey() != null && !config.getGeminiApiKey().isBlank();
    }

    private Map<String, Object> body(String prompt) {
        String text = (config.getSystemPrompt() == null ? "" : config.getSystemPrompt() + "\n\n")
            + (prompt == null ? "" : prompt);
        return Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", text)))),
            "generationConfig", Map.of("temperature", config.getTemperature()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String think(String prompt) {
        if (!isAvailable()) {
            return "";
        }
        try {
            Map<String, Object> res = client.post()
                .uri("/models/{m}:generateContent?key={k}", config.getGeminiModel(), config.getGeminiApiKey())
                .body(body(prompt))
                .retrieve().body(Map.class);
            return extractText(res);
        } catch (RuntimeException ex) {
            log.warn("Gemini call failed ({}); degrading", ex.getMessage());
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
            String json = MAPPER.writeValueAsString(body(prompt));
            String url = BASE + "/models/" + URLEncoder.encode(config.getGeminiModel(), StandardCharsets.UTF_8)
                + ":streamGenerateContent?alt=sse&key=" + URLEncoder.encode(config.getGeminiApiKey(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<java.io.InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) continue;
                    String tok = extractText(MAPPER.readValue(data, Map.class));
                    if (tok != null && !tok.isEmpty()) {
                        full.append(tok);
                        onToken.accept(tok);
                    }
                }
            }
            if (full.length() > 0) {
                return full.toString().strip();
            }
        } catch (Exception ex) {
            log.warn("Gemini streaming failed ({}); falling back", ex.getMessage());
        }
        String a = think(prompt);
        if (a != null && !a.isBlank() && full.length() == 0) {
            onToken.accept(a);
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    private static String extractText(Map<String, Object> res) {
        List<Map<String, Object>> candidates = res == null ? null : (List<Map<String, Object>>) res.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = content == null ? null : (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        Object t = parts.get(0).get("text");
        return t == null ? "" : t.toString();
    }
}
