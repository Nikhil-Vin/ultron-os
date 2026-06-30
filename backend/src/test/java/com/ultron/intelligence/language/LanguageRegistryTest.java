package com.ultron.intelligence.language;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Language registry + onboarding on in-memory H2. Verifies the launch languages seed and that a
 * new language onboards as a registry entry with an honest quality tier.
 */
@DataJpaTest
@Import({LanguageRegistry.class, LanguageOnboardingService.class})
class LanguageRegistryTest {

    @Autowired
    private LanguageRegistry registry;

    @Autowired
    private LanguageOnboardingService onboarding;

    @Test
    void seedsLaunchLanguages() {
        assertThat(registry.byCode("en")).isPresent();
        assertThat(registry.byCode("hi")).isPresent();
        assertThat(registry.byCode("mr")).isPresent();
        assertThat(registry.byCode("hi").get().getNativeName()).isEqualTo("हिन्दी");
        assertThat(registry.byCode("mr").get().getLlmQualityTier()).isEqualTo("experimental");
    }

    @Test
    void onboardsNewLanguageAsRegistryEntry() {
        LanguageOnboardingService.OnboardingReport report =
            onboarding.onboard("ta", "Tamil", "தமிழ்", "Tamil", null);

        assertThat(report.added()).isTrue();
        assertThat(report.sttCoverage()).isEqualTo("moderate");
        assertThat(report.localTts()).isFalse();           // no local Piper voice supplied
        assertThat(registry.isSupported("ta")).isTrue();
    }

    @Test
    void onboardingExistingLanguageIsIdempotent() {
        LanguageOnboardingService.OnboardingReport report =
            onboarding.onboard("en", "English", "English", "Latin", "en_US-amy-medium");
        assertThat(report.added()).isFalse();
    }
}
