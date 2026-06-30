package com.ultron.api;

import com.ultron.intelligence.language.Language;
import com.ultron.intelligence.language.LanguageOnboardingService;
import com.ultron.intelligence.language.LanguageRegistry;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Language API (L0 — Section 9.10). List supported languages with honest quality tiers, and
 * onboard a new one (registry entry; no pipeline code change for whisper-covered languages).
 */
@RestController
@RequestMapping("/api/languages")
public class LanguageController {

    private final LanguageRegistry registry;
    private final LanguageOnboardingService onboarding;
    private final com.ultron.intelligence.language.LanguagePreference preference;

    public LanguageController(LanguageRegistry registry, LanguageOnboardingService onboarding,
                              com.ultron.intelligence.language.LanguagePreference preference) {
        this.registry = registry;
        this.onboarding = onboarding;
        this.preference = preference;
    }

    @GetMapping("/active")
    public java.util.Map<String, String> activeLanguage() {
        return java.util.Map.of("code", preference.getCode());
    }

    @PostMapping("/active")
    public java.util.Map<String, String> setActive(@RequestBody ActiveRequest req) {
        preference.setCode(req.code());
        return java.util.Map.of("code", preference.getCode());
    }

    public record ActiveRequest(@NotBlank String code) {
    }

    @GetMapping
    public List<LanguageDto> list() {
        return registry.all().stream().map(LanguageDto::from).toList();
    }

    @PostMapping
    public ResponseEntity<LanguageOnboardingService.OnboardingReport> add(@RequestBody AddLanguageRequest req) {
        return ResponseEntity.ok(onboarding.onboard(
            req.code(), req.displayName(), req.nativeName(), req.script(), req.ttsVoiceId()));
    }

    public record AddLanguageRequest(
        @NotBlank String code,
        @NotBlank String displayName,
        String nativeName,
        String script,
        String ttsVoiceId) {
    }

    public record LanguageDto(
        String code, String displayName, String nativeName, String script,
        String ttsEngine, String ttsVoiceId, String qualityTier, boolean enabled) {

        static LanguageDto from(Language l) {
            return new LanguageDto(l.getLanguageCode(), l.getDisplayName(), l.getNativeName(),
                l.getScript(), l.getTtsEngine(), l.getTtsVoiceId(), l.getLlmQualityTier(), l.isEnabled());
        }
    }
}
