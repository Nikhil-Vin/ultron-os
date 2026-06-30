package com.ultron.intelligence.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.ultron.config.PythonBridgeConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the opt-in Python bridge client against an in-process stub FastAPI — no network.
 * Confirms it stays inert when disabled, reaches the service when enabled, parses embeddings,
 * sends the API key, and degrades to empty on error.
 */
class PythonBridgeClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
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

    private int startStub(AtomicReference<String> capturedKey) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/health", ex -> {
            if (capturedKey != null) {
                capturedKey.set(ex.getRequestHeaders().getFirst("X-API-Key"));
            }
            send(ex, 200, "{\"status\":\"ok\"}");
        });
        server.createContext("/embed", ex ->
            send(ex, 200, "{\"backend\":\"hashing\",\"dim\":3,\"vector\":[0.1,0.2,0.3]}"));
        server.start();
        return server.getAddress().getPort();
    }

    @Test
    void inertWhenDisabled() throws IOException {
        int port = startStub(null);
        PythonBridgeConfig cfg = new PythonBridgeConfig();
        cfg.setEnabled(false); // default
        cfg.setBaseUrl("http://localhost:" + port);
        PythonBridgeClient client = new PythonBridgeClient(cfg);

        assertThat(client.isAvailable()).isFalse();
        assertThat(client.embed("anything")).isEmpty();
    }

    @Test
    void reachableAndEmbedsWhenEnabled() throws IOException {
        AtomicReference<String> key = new AtomicReference<>();
        int port = startStub(key);
        PythonBridgeConfig cfg = new PythonBridgeConfig();
        cfg.setEnabled(true);
        cfg.setBaseUrl("http://localhost:" + port);
        cfg.setApiKey("secret123");
        PythonBridgeClient client = new PythonBridgeClient(cfg);

        assertThat(client.isAvailable()).isTrue();
        assertThat(key.get()).isEqualTo("secret123"); // API key forwarded

        float[] vec = client.embed("deploy the frontend");
        assertThat(vec).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void degradesToEmptyWhenServiceErrors() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/health", ex -> send(ex, 200, "{\"status\":\"ok\"}"));
        server.createContext("/embed", ex -> send(ex, 500, "{\"detail\":\"boom\"}"));
        server.start();
        int port = server.getAddress().getPort();

        PythonBridgeConfig cfg = new PythonBridgeConfig();
        cfg.setEnabled(true);
        cfg.setBaseUrl("http://localhost:" + port);
        PythonBridgeClient client = new PythonBridgeClient(cfg);

        assertThat(client.isAvailable()).isTrue();
        assertThat(client.embed("x")).isEmpty(); // 500 → empty, no throw
    }

    @Test
    void intakeSkillReturnsChunkMetadataWhenEnabled() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/health", ex -> send(ex, 200, "{\"status\":\"ok\"}"));
        server.createContext("/skills/intake", ex ->
            send(ex, 200, "{\"name\":\"doc\",\"format\":\"web\",\"chunk_count\":3}"));
        server.start();
        int port = server.getAddress().getPort();

        PythonBridgeConfig cfg = new PythonBridgeConfig();
        cfg.setEnabled(true);
        cfg.setBaseUrl("http://localhost:" + port);
        PythonBridgeClient client = new PythonBridgeClient(cfg);

        var result = client.intakeSkill("doc", null, "https://example.com/article", "text/html");
        assertThat(result).containsEntry("format", "web");
        assertThat(((Number) result.get("chunk_count")).intValue()).isEqualTo(3);
    }

    @Test
    void intakeSkillEmptyWhenDisabled() {
        PythonBridgeConfig cfg = new PythonBridgeConfig(); // disabled
        PythonBridgeClient client = new PythonBridgeClient(cfg);
        assertThat(client.intakeSkill("doc", null, "https://x.io", "text/html")).isEmpty();
    }
}
