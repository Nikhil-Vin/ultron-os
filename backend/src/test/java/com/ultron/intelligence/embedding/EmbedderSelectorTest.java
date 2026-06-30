package com.ultron.intelligence.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.ultron.config.UltronProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies fail-safe degradation (Section 4): when the Ollama server is reachable but cannot
 * embed (e.g. started without {@code --embeddings}), the selector must still return a usable
 * vector from the heuristic embedder rather than throwing.
 */
class EmbedderSelectorTest {

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

    /** Server: /api/tags OK (so it looks available) but /api/embeddings returns the unsupported error. */
    private int startServerThatCannotEmbed() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/tags", ex -> send(ex, 200, "{\"models\":[]}"));
        server.createContext("/api/embeddings", ex ->
            send(ex, 500, "{\"error\":\"This server does not support embeddings. Start it with `--embeddings`\"}"));
        server.start();
        return server.getAddress().getPort();
    }

    @Test
    void degradesToHeuristicWhenOllamaCannotEmbed() throws IOException {
        int port = startServerThatCannotEmbed();
        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaBaseUrl("http://localhost:" + port);
        props.getBrain().setOllamaModel("llama3.1");

        OllamaEmbedder ollama = new OllamaEmbedder(props);
        HeuristicEmbedder heuristic = new HeuristicEmbedder();
        EmbedderSelector selector = new EmbedderSelector(ollama, heuristic);

        // First call: Ollama is "available" (tags ok) but embedding 500s → must fall back, not throw.
        float[] vec = selector.embed("deploy the frontend");
        assertThat(vec).hasSize(HeuristicEmbedder.DIMENSION);

        // After the failed embed, Ollama latches unavailable → active embedder is now heuristic.
        assertThat(selector.active().name()).isEqualTo("heuristic");

        // Subsequent embeds keep working via the heuristic.
        assertThat(selector.embed("another query")).hasSize(HeuristicEmbedder.DIMENSION);
    }

    @Test
    void usesHeuristicDirectlyWhenOllamaDisabled() {
        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaEnabled(false);
        EmbedderSelector selector =
            new EmbedderSelector(new OllamaEmbedder(props), new HeuristicEmbedder());
        assertThat(selector.active().name()).isEqualTo("heuristic");
        assertThat(selector.embed("hello world")).hasSize(HeuristicEmbedder.DIMENSION);
    }
}
