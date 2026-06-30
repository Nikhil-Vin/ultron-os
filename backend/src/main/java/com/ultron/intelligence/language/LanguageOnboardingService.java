package com.ultron.intelligence.language;

import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Add-a-language workflow (Section 9.10.D). Checks STT coverage (whisper covers 90+ languages out
 * of the box, so STT is usually free), locates/notes a TTS voice, and registers the language with
 * an honest quality tier. No pipeline code changes are needed for a whisper-covered language.
 */
@Service
public class LanguageOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(LanguageOnboardingService.class);

    /** Languages whisper's multilingual checkpoint covers well out of the box (subset; extend freely). */
    private static final Set<String> WHISPER_STRONG = Set.of(
        "en", "hi", "es", "fr", "de", "it", "pt", "nl", "ja", "ko", "zh", "ru", "ar", "tr");
    private static final Set<String> WHISPER_MODERATE = Set.of("mr", "ta", "te", "bn", "gu", "kn", "ml", "pa");

    private final LanguageRegistry registry;

    public LanguageOnboardingService(LanguageRegistry registry) {
        this.registry = registry;
    }

    /**
     * Onboard a new language. Returns a report describing STT/TTS coverage and the assigned tier.
     *
     * @param code        ISO 639-1 code
     * @param displayName e.g. "Tamil"
     * @param nativeName  e.g. "தமிழ்"
     * @param script      e.g. "Tamil"
     * @param ttsVoiceId  a local Piper voice id if available, else null (→ cloud TTS opt-in or text-only)
     */
    public OnboardingReport onboard(String code, String displayName, String nativeName,
                                    String script, String ttsVoiceId) {
        String c = code == null ? "" : code.toLowerCase();
        if (c.isBlank()) {
            throw new IllegalArgumentException("language code is required");
        }
        if (registry.isSupported(c)) {
            return new OnboardingReport(c, false, "already-registered", sttCoverage(c), ttsVoiceId != null, tierFor(c));
        }

        boolean hasLocalTts = ttsVoiceId != null && !ttsVoiceId.isBlank();
        String tier = tierFor(c);
        String ttsEngine = hasLocalTts ? "piper" : "azure"; // no local voice → suggest opt-in cloud TTS

        registry.register(new Language(
            UUID.randomUUID(), c, displayName, nativeName, script,
            "whisper-multilingual", ttsEngine, hasLocalTts ? ttsVoiceId : null,
            "rules/" + c + ".json", tier, true));

        String note = hasLocalTts
            ? "Registered with local Piper voice."
            : "Registered. No local TTS voice found — enable cloud TTS (Azure/ElevenLabs, opt-in) "
                + "or run text-only for this language until a Piper voice is added.";
        log.info("Onboarded language {} ({}), tier={}, localTts={}", c, displayName, tier, hasLocalTts);
        return new OnboardingReport(c, true, note, sttCoverage(c), hasLocalTts, tier);
    }

    private String sttCoverage(String code) {
        if (WHISPER_STRONG.contains(code)) return "strong";
        if (WHISPER_MODERATE.contains(code)) return "moderate";
        return "check-whisper"; // whisper likely covers it; verify on first use
    }

    private String tierFor(String code) {
        if (WHISPER_STRONG.contains(code)) return "good";
        if (WHISPER_MODERATE.contains(code)) return "experimental";
        return "experimental";
    }

    /**
     * Outcome of an onboarding attempt.
     *
     * @param code        the language code
     * @param added       true if newly registered
     * @param note        human-readable guidance (esp. about TTS)
     * @param sttCoverage strong | moderate | check-whisper
     * @param localTts    true if a local Piper voice was assigned
     * @param qualityTier honest LLM/voice quality tier
     */
    public record OnboardingReport(String code, boolean added, String note,
                                   String sttCoverage, boolean localTts, String qualityTier) {
    }
}
