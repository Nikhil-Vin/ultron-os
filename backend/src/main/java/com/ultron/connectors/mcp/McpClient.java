package com.ultron.connectors.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Base Model Context Protocol client (L5 — Section 3 "open standard connectors"). Speaks MCP's
 * JSON-RPC 2.0 over HTTP to an MCP server, exposing {@code listTools} and {@code callTool}. This is
 * the open-standard seam every app connector can ride on instead of bespoke plugin formats.
 *
 * <p>Stdio-transport MCP servers are launched + bridged separately; this HTTP client targets
 * servers exposing the streamable-HTTP transport.
 */
@Component
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicLong rpcId = new AtomicLong(1);

    /** List tools advertised by an MCP server. */
    public Object listTools(String serverUrl) {
        return rpc(serverUrl, "tools/list", Map.of());
    }

    /** Call a named tool on an MCP server with arguments. */
    public Object callTool(String serverUrl, String tool, Map<String, Object> args) {
        return rpc(serverUrl, "tools/call", Map.of("name", tool, "arguments", args));
    }

    @SuppressWarnings("unchecked")
    public Object rpc(String serverUrl, String method, Map<String, Object> params) {
        Map<String, Object> body = Map.of(
            "jsonrpc", "2.0",
            "id", rpcId.getAndIncrement(),
            "method", method,
            "params", params);
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> parsed = mapper.readValue(res.body(), Map.class);
            if (parsed.containsKey("error")) {
                log.warn("MCP error from {}: {}", serverUrl, parsed.get("error"));
            }
            return parsed.getOrDefault("result", parsed);
        } catch (Exception ex) {
            log.warn("MCP call {} -> {} failed: {}", method, serverUrl, ex.getMessage());
            return Map.of("error", ex.getMessage());
        }
    }
}
