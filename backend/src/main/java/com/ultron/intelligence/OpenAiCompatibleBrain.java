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
import org.springframework.web.client.RestClient;

/**
 * Base for any OpenAI-compatible chat-completions provider (OpenAI, Groq, OpenRouter, GitHub
 * Models, NVIDIA NIM, …). Subclasses supply a name, base URL, model, and bearer key. Inert when the
 * key is blank, so every provider is opt-in. Supports real SSE token streaming via {@code stream:true}.
 */
public abstract class OpenAiCompatibleBrain implements Brain {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleBrain.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final String baseUrl;
    private final RestClient client;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    protected final UltronProperties.Brain config;

    protected OpenAiCompatibleBrain(String name, String baseUrl, UltronProperties.Brain config) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.config = config;
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(3))
            .withReadTimeout(Duration.ofSeconds(60));
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
        this.client = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    protected abstract String apiKey();

    /** Public so {@link BrainSelector} can report the model in health. */
    public abstract String model();

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isAvailable() {
        String k = apiKey();
        return k != null && !k.isBlank();
    }

    private Map<String, Object> body(String prompt, boolean stream) {
        return Map.of(
            "model", model(),
            "temperature", config.getTemperature(),
            "stream", stream,
            "messages", List.of(
                Map.of("role", "system", "content", config.getSystemPrompt()),
                Map.of("role", "user", "content", prompt == null ? "" : prompt)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String think(String prompt) {
        if (!isAvailable()) {
            return "";
        }
        try {
            Map<String, Object> res = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey())
                .body(body(prompt, false))
                .retrieve().body(Map.class);
            List<Map<String, Object>> choices = res == null ? null : (List<Map<String, Object>>) res.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            Object content = msg == null ? null : msg.get("content");
            return content == null ? "" : content.toString().strip();
        } catch (RuntimeException ex) {
            log.warn("{} call failed ({}); degrading", name, ex.getMessage());
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
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<java.io.InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) {
                        if ("[DONE]".equals(data)) break;
                        continue;
                    }
                    Map<String, Object> chunk = MAPPER.readValue(data, Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                    if (choices == null || choices.isEmpty()) continue;
                    Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                    Object tok = delta == null ? null : delta.get("content");
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
            log.warn("{} streaming failed ({}); falling back", name, ex.getMessage());
        }
        // fallback to non-streaming
        String a = think(prompt);
        if (a != null && !a.isBlank() && full.length() == 0) {
            onToken.accept(a);
        }
        return a;
    }
}
