package com.ultron.intelligence.language;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Language registry (L3 — Section 9.10.D). Source of truth for what Ultron currently speaks.
 * Seeds the three launch languages (English/Hindi/Marathi) on startup if the table is empty, so
 * both Postgres and the in-memory H2 test profile have them. Quality tiers are honest.
 */
@Service
public class LanguageRegistry {

    private static final Logger log = LoggerFactory.getLogger(LanguageRegistry.class);

    private final LanguageRepository repository;

    public LanguageRegistry(LanguageRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void seedDefaults() {
        if (repository.count() > 0) {
            return;
        }
        register(new Language(UUID.randomUUID(), "en", "English", "English", "Latin",
            "whisper-multilingual", "piper", "en_US-amy-medium", "rules/en.json", "native", true));
        register(new Language(UUID.randomUUID(), "hi", "Hindi", "हिन्दी", "Devanagari",
            "whisper-multilingual", "piper", "hi_IN-pratham-medium", "rules/hi.json", "good", true));
        register(new Language(UUID.randomUUID(), "mr", "Marathi", "मराठी", "Devanagari",
            "whisper-multilingual", "piper", "mr_IN-placeholder", "rules/mr.json", "experimental", true));
        log.info("Seeded launch languages: en (native), hi (good), mr (experimental)");
    }

    @Transactional(readOnly = true)
    public List<Language> all() {
        return repository.findByEnabledTrueOrderByDisplayNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Language> byCode(String code) {
        return repository.findByLanguageCode(code == null ? "" : code.toLowerCase());
    }

    @Transactional
    public Language register(Language language) {
        log.info("Registering language code={} tier={}", language.getLanguageCode(), language.getLlmQualityTier());
        return repository.save(language);
    }

    @Transactional
    public boolean disable(String code) {
        Optional<Language> lang = byCode(code);
        lang.ifPresent(l -> {
            l.setEnabled(false);
            repository.save(l);
        });
        return lang.isPresent();
    }

    public boolean isSupported(String code) {
        return repository.existsByLanguageCode(code == null ? "" : code.toLowerCase());
    }
}
