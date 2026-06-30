package com.ultron.devices;

import com.ultron.governance.RiskLevel;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Fast intent → action lookup (Section 4 latency). Simple device commands ("open youtube", "lock
 * screen") resolve here in microseconds and skip the full LLM agent loop entirely; only ambiguous
 * commands fall through to the brain. Each match carries its {@link RiskLevel} so the gate applies.
 */
@Component
public class CommandRouter {

    /** A resolved device command. */
    public record Parsed(String deviceType, String action, Map<String, Object> args, RiskLevel risk) {
    }

    /** Try to resolve a phrase to a device command. Returns null if it needs the brain. */
    public Parsed route(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return null;
        }
        String t = phrase.toLowerCase(Locale.ROOT).strip();
        String device = t.contains("laptop") || t.contains("computer") || t.contains("pc") ? "laptop"
            : t.contains("phone") || t.contains("mobile") ? "android" : null;

        // --- CRITICAL (contact/money) ---
        if (t.startsWith("call ") || t.contains("call mom") || t.contains("dial ")) {
            return new Parsed(dev(device, "android"), "call", arg("contact", after(t, "call")), RiskLevel.CRITICAL);
        }
        if (t.contains("whatsapp") || t.contains("send sms") || t.contains("text ")) {
            return new Parsed(dev(device, "android"), "message", arg("raw", t), RiskLevel.CRITICAL);
        }

        // --- READ (instant, no gate) ---
        if (t.contains("battery")) return new Parsed(dev(device, "android"), "battery", Map.of(), RiskLevel.READ);
        if (t.contains("notification")) return new Parsed(dev(device, "android"), "notifications", Map.of(), RiskLevel.READ);
        if (t.contains("screenshot") || t.contains("screen shot")) return new Parsed(dev(device, "android"), "screenshot", Map.of(), RiskLevel.READ);
        if (t.contains("running apps") || t.contains("what apps")) return new Parsed("laptop", "processes", Map.of(), RiskLevel.READ);
        if (t.contains("cpu") || t.contains("ram") || t.contains("memory usage")) return new Parsed("laptop", "sysinfo", Map.of(), RiskLevel.READ);
        if (t.startsWith("open ") || t.contains("launch ")) {
            String app = after(t, t.contains("launch") ? "launch" : "open").replace(" on my phone", "").replace(" on my laptop", "").trim();
            return new Parsed(dev(device, "android"), "open_app", arg("app", app), RiskLevel.READ);
        }
        if (t.contains("camera")) return new Parsed(dev(device, "android"), "open_app", arg("app", "camera"), RiskLevel.READ);

        // --- LOW (logged) ---
        if (t.contains("lock")) return new Parsed(dev(device, "laptop"), "lock", Map.of(), RiskLevel.LOW);
        if (t.contains("flashlight") || t.contains("torch")) return new Parsed("android", "flashlight", arg("on", t.contains("on")), RiskLevel.LOW);
        if (t.contains("do not disturb") || t.contains("dnd")) return new Parsed("android", "dnd", arg("on", !t.contains("off")), RiskLevel.LOW);
        if (t.contains("alarm")) return new Parsed("android", "alarm", arg("raw", t), RiskLevel.LOW);
        if (t.contains("mute")) return new Parsed("laptop", "mute", arg("on", !t.contains("unmute")), RiskLevel.LOW);
        if (t.contains("play music") || t.contains("play song")) return new Parsed(dev(device, "android"), "media_play", Map.of(), RiskLevel.LOW);

        // --- HIGH (approval) ---
        if (t.contains("navigate") || t.contains("directions") || t.contains("maps to")) {
            return new Parsed("android", "navigate", arg("destination", after(t, "to")), RiskLevel.HIGH);
        }
        if (t.contains("type ")) return new Parsed("laptop", "type", arg("text", after(t, "type")), RiskLevel.HIGH);

        return null; // → let the brain handle it
    }

    private static String dev(String resolved, String fallback) {
        return resolved != null ? resolved : fallback;
    }

    private static Map<String, Object> arg(String k, Object v) {
        Map<String, Object> m = new HashMap<>();
        m.put(k, v);
        return m;
    }

    private static String after(String t, String kw) {
        int i = t.indexOf(kw);
        return i < 0 ? "" : t.substring(i + kw.length()).strip();
    }
}
