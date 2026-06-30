package com.ultron.devices;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Registry of connected devices (L5). Tracks each device's WebSocket session, type, capabilities,
 * and online status, and dispatches commands to them in real time. The "hands" of Ultron reach
 * every registered device over its persistent socket.
 */
@Component
public class DeviceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistry.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    /** A connected device. */
    public static final class Device {
        public final String id;
        public final String type; // android | laptop | browser
        public volatile List<String> capabilities = List.of();
        public volatile Instant lastSeen = Instant.now();
        public volatile boolean online = true;
        final WebSocketSession session;

        Device(String id, String type, WebSocketSession session) {
            this.id = id;
            this.type = type;
            this.session = session;
        }
    }

    public Device register(String type, WebSocketSession session) {
        String id = type + "-" + session.getId().substring(0, Math.min(8, session.getId().length()));
        Device d = new Device(id, type, session);
        devices.put(id, d);
        log.info("Device connected: {} ({})", id, type);
        return d;
    }

    public void update(String id, List<String> capabilities) {
        Device d = devices.get(id);
        if (d != null) {
            d.capabilities = capabilities;
            d.lastSeen = Instant.now();
        }
    }

    public void disconnect(WebSocketSession session) {
        devices.values().stream()
            .filter(d -> d.session.getId().equals(session.getId()))
            .forEach(d -> { d.online = false; log.info("Device disconnected: {}", d.id); });
        devices.values().removeIf(d -> !d.online);
    }

    public List<Map<String, Object>> list() {
        return devices.values().stream().map(d -> Map.<String, Object>of(
            "id", d.id, "type", d.type, "online", d.online,
            "capabilities", d.capabilities, "lastSeen", d.lastSeen.toString())).toList();
    }

    public boolean isOnline(String type) {
        return devices.values().stream().anyMatch(d -> d.type.equals(type) && d.online);
    }

    /** Send a command to a specific device. Returns a command id, or null if not reachable. */
    public String sendCommand(String deviceId, String action, Map<String, Object> args, String approvalToken) {
        Device d = devices.get(deviceId);
        if (d == null || !d.online || !d.session.isOpen()) {
            return null;
        }
        String cmdId = UUID.randomUUID().toString();
        try {
            String json = mapper.writeValueAsString(Map.of(
                "id", cmdId, "action", action, "args", args == null ? Map.of() : args,
                "approvalToken", approvalToken == null ? "" : approvalToken));
            d.session.sendMessage(new TextMessage(json));
            return cmdId;
        } catch (IOException ex) {
            log.warn("sendCommand to {} failed: {}", deviceId, ex.getMessage());
            return null;
        }
    }

    /** Send to the first online device of a type (e.g. "android"). */
    public String sendToType(String type, String action, Map<String, Object> args, String approvalToken) {
        return devices.values().stream()
            .filter(d -> d.type.equals(type) && d.online)
            .findFirst()
            .map(d -> sendCommand(d.id, action, args, approvalToken))
            .orElse(null);
    }
}
