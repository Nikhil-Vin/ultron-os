package com.ultron.connectors;

/**
 * Uniform connector result (L5). {@code connected=false} means the connector has no credentials;
 * {@code ok=false} with {@code connected=true} means an operation was blocked (e.g. by the gate) or
 * failed. {@code data} carries any payload (lists, ids, summaries).
 */
public record ConnectorResponse(boolean connected, boolean ok, String message, Object data) {

    public static ConnectorResponse notConnected(String name) {
        return new ConnectorResponse(false, false, name + " is not connected (set its token to enable).", null);
    }

    public static ConnectorResponse ok(String message, Object data) {
        return new ConnectorResponse(true, true, message, data);
    }

    public static ConnectorResponse blocked(String message) {
        return new ConnectorResponse(true, false, message, null);
    }
}
