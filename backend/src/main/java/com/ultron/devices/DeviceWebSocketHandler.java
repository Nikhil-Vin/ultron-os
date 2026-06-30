package com.ultron.devices;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket endpoint {@code /ws/device/{type}} (type = android | laptop | browser). Devices connect
 * with a persistent socket, register their capabilities, receive commands, and return results.
 * Bidirectional + low-latency — the transport behind Siri-like device control.
 */
@Component
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceWebSocketHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final DeviceRegistry registry;

    public DeviceWebSocketHandler(DeviceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String type = pathType(session);
        DeviceRegistry.Device d = registry.register(type, session);
        session.getAttributes().put("deviceId", d.id);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) {
        try {
            Map<String, Object> msg = mapper.readValue(message.getPayload(), Map.class);
            String deviceId = (String) session.getAttributes().get("deviceId");
            String type = (String) msg.get("type");
            if ("register".equals(type)) {
                Object caps = msg.get("capabilities");
                registry.update(deviceId, caps instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of());
            } else if ("result".equals(type)) {
                log.info("Device {} result for {}: {}", deviceId, msg.get("id"), msg.get("result"));
            }
        } catch (Exception ex) {
            log.warn("device ws message error: {}", ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.disconnect(session);
    }

    private static String pathType(WebSocketSession session) {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        int i = path.lastIndexOf('/');
        return i >= 0 ? path.substring(i + 1) : "unknown";
    }

    /** Registers the device WebSocket handler. */
    @Component
    @EnableWebSocket
    public static class WsConfig implements WebSocketConfigurer {
        private final DeviceWebSocketHandler handler;

        public WsConfig(DeviceWebSocketHandler handler) {
            this.handler = handler;
        }

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            registry.addHandler(handler, "/ws/device/{type}").setAllowedOriginPatterns("*");
        }
    }
}
