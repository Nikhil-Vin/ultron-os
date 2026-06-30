package com.ultron.intelligence.embedding;

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
 * Verifies the Ollama embedder against an in-process stub server — no real Ollama, no network.
 * Confirms it posts the dedicated embedding model, parses the embedding vector, and degrades when
 * the server reports embeddings are unsupported.
 */
class OllamaEmbedderTest {

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

    private UltronProperties propsFor(int port) {
        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaBaseUrl("http://localhost:" + port);
        props.getBrain().setOllamaModel("llama3.1");
        props.getBrain().setOllamaEmbeddingModel("nomic-embed-text");
        return props;
    }

    @Test
    void postsEmbeddingModelAndParsesVector() throws IOException {
        AtomicReference<String> captured = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/tags", ex -> send(ex, 200, "{\"models\":[]}"));
        server.createContext("/api/embeddings", ex -> {
            captured.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            send(ex, 200, "{\"embedding\":[3.0,4.0]}"); // non-unit → checks normalisation
        });
        server.start();
        int port = server.getAddress().getPort();

        OllamaEmbedder embedder = new OllamaEmbedder(propsFor(port));

        assertThat(embedder.isAvailable()).isTrue();
        assertThat(embedder.model()).isEqualTo("nomic-embed-text");

        float[] vec = embedder.embed("hello world");
        // [3,4] L2-normalised → [0.6, 0.8]
        assertThat(vec).hasSize(2);
        assertThat(vec[0]).isCloseTo(0.6f, org.assertj.core.data.Offset.offset(1e-5f));
        assertThat(vec[1]).isCloseTo(0.8f, org.assertj.core.data.Offset.offset(1e-5f));
        // The request used the embedding model, not the chat model.
        assertThat(captured.get()).contains("\"model\":\"nomic-embed-text\"").contains("hello world");
    }

    @Test
    void latchesUnavailableWhenEmbeddingsUnsupported() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/tags", ex -> send(ex, 200, "{\"models\":[]}"));
        server.createContext("/api/embeddings", ex ->
            send(ex, 500, "{\"error\":\"This server does not support embeddings. Start it with `--embeddings`\"}"));
        server.start();
        int port = server.getAddress().getPort();

        OllamaEmbedder embedder = new OllamaEmbedder(propsFor(port));
        assertThat(embedder.isAvailable()).isTrue();        // before any embed
        assertThat(embedder.embed("x")).isEmpty();          // 500 → empty, no throw
        assertThat(embedder.isAvailable()).isFalse();       // latched off afterwards
    }
}
