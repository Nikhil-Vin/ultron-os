package com.ultron.intelligence.voice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Voice profile manager (Section 9.9.C). Holds the selectable TTS voice profiles and the active
 * selection. Local Piper profiles are free + offline; ElevenLabs / cloud / clone profiles are
 * opt-in adapters behind the same contract (the voice agent reads the active profile to pick its
 * engine + voice id). Switching is possible by settings or voice command ("use the warm voice").
 */
@Component
public class VoiceProfileManager {

    private static final Logger log = LoggerFactory.getLogger(VoiceProfileManager.class);

    /** A selectable voice profile. {@code paid} marks opt-in cloud engines. */
    public record VoiceProfile(String id, String engine, String voiceId, String character, boolean paid) {
    }

    private final Map<String, VoiceProfile> profiles = new LinkedHashMap<>();
    private final AtomicReference<String> activeId = new AtomicReference<>();

    public VoiceProfileManager() {
        register(new VoiceProfile("default", "piper", "en_US-amy-medium", "Neutral, calm", false));
        register(new VoiceProfile("warm", "piper", "en_US-hfc_female-medium", "Friendlier cadence", false));
        register(new VoiceProfile("sharp", "piper", "en_US-ryan-high", "Fast, clipped — TRADING/FOCUS", false));
        register(new VoiceProfile("premium", "elevenlabs", "Rachel", "Near-human prosody (opt-in)", true));
        register(new VoiceProfile("cloud-hd", "azure", "en-US-AvaNeural", "Cloud neural (opt-in)", true));
        register(new VoiceProfile("clone", "xtts", "custom", "Voice clone from sample (opt-in)", false));
        activeId.set("default");
    }

    private void register(VoiceProfile p) {
        profiles.put(p.id(), p);
    }

    public List<VoiceProfile> all() {
        return List.copyOf(profiles.values());
    }

    public VoiceProfile active() {
        return profiles.get(activeId.get());
    }

    /** Select a profile by id. Returns the new active profile, or throws if unknown. */
    public VoiceProfile select(String id) {
        VoiceProfile p = profiles.get(id == null ? "" : id.toLowerCase());
        if (p == null) {
            throw new IllegalArgumentException("Unknown voice profile: " + id);
        }
        activeId.set(p.id());
        log.info("Voice profile -> {} ({}:{})", p.id(), p.engine(), p.voiceId());
        return p;
    }

    /**
     * Resolve a profile from a spoken phrase ("switch to the warm voice", "use a deeper voice").
     * Returns the selected profile, or null if no profile keyword is present.
     */
    public VoiceProfile selectFromPhrase(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return null;
        }
        String t = phrase.toLowerCase();
        String id = null;
        if (t.contains("warm")) id = "warm";
        else if (t.contains("sharp") || t.contains("clipped") || t.contains("fast")) id = "sharp";
        else if (t.contains("premium") || t.contains("natural") || t.contains("human")) id = "premium";
        else if (t.contains("cloud")) id = "cloud-hd";
        else if (t.contains("clone") || t.contains("my voice")) id = "clone";
        else if (t.contains("default") || t.contains("normal")) id = "default";
        return id == null ? null : select(id);
    }
}
