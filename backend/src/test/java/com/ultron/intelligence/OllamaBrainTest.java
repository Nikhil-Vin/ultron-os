package com.ultron.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.ultron.config.UltronProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Ollama brain against an in-process stub HTTP server — no real Ollama, no network.
 * Covers availability probing, persona/option assembly, response parsing, and fail-safe fallback.
 */
class OllamaBrainTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private UltronProperties propsFor(int port) {
        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaBaseUrl("http://localhost:" + port);
        props.getBrain().setOllamaModel("test-model");
        props.getBrain().setSystemPrompt("You are Ultron.");
        props.getBrain().setAvailabilityCacheMillis(0); // no caching between probes in tests
        return props;
    }

    private static void send(com.sun.net.httpserver.HttpExchange ex, int status, String json)
        throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private int startServer(boolean tagsOk, int generateStatus, String generateBody,
                            AtomicReference<String> capturedRequest) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/tags", ex -> {
            if (tagsOk) {
                send(ex, 200, "{\"models\":[]}");
            } else {
                send(ex, 500, "{}");
            }
        });
        server.createContext("/api/generate", ex -> {
            String reqBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (capturedRequest != null) {
                capturedRequest.set(reqBody);
            }
            send(ex, generateStatus, generateBody);
        });
        server.start();
        return server.getAddress().getPort();
    }

    @Test
    void availableWhenTagsRespond() throws IOException {
        int port = startServer(true, 200, "{\"response\":\"ok\"}", null);
        OllamaBrain brain = new OllamaBrain(propsFor(port));
        assertThat(brain.isAvailable()).isTrue();
        assertThat(brain.name()).isEqualTo("ollama");
        assertThat(brain.model()).isEqualTo("test-model");
    }

    @Test
    void notAvailableWhenDisabled() throws IOException {
        int port = startServer(true, 200, "{\"response\":\"ok\"}", null);
        UltronProperties props = propsFor(port);
        props.getBrain().setOllamaEnabled(false);
        OllamaBrain brain = new OllamaBrain(props);
        assertThat(brain.isAvailable()).isFalse();
    }

    @Test
    void notAvailableWhenServerDown() throws IOException {
        int port = startServer(false, 500, "{}", null);
        OllamaBrain brain = new OllamaBrain(propsFor(port));
        assertThat(brain.isAvailable()).isFalse();
    }

    @Test
    void thinkSendsPersonaAndParsesResponse() throws IOException {
        AtomicReference<String> captured = new AtomicReference<>();
        int port = startServer(true, 200, "{\"response\":\"  Hello from Ultron  \"}", captured);
        OllamaBrain brain = new OllamaBrain(propsFor(port));

        String answer = brain.think("What is my deploy command?");

        assertThat(answer).isEqualTo("Hello from Ultron"); // trimmed
        assertThat(captured.get())
            .contains("\"model\":\"test-model\"")
            .contains("\"system\":\"You are Ultron.\"")
            .contains("\"stream\":false")
            .contains("What is my deploy command?");
    }

    @Test
    void thinkReturnsBlankOnServerErrorForFailSafeFallback() throws IOException {
        int port = startServer(true, 500, "{}", null);
        OllamaBrain brain = new OllamaBrain(propsFor(port));

        // isAvailable is true (tags ok) but generation fails → blank, so BrainSelector degrades.
        assertThat(brain.isAvailable()).isTrue();
        assertThat(brain.think("anything")).isEmpty();
    }
}
