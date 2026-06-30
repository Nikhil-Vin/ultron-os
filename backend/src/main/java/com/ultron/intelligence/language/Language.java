package com.ultron.intelligence.language;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A language Ultron supports (L3 — Section 9.10). Maps {@code supported_languages}. Quality tier is
 * labelled honestly ({@code native}/{@code good}/{@code experimental}) so the UI sets expectations.
 */
@Entity
@Table(name = "supported_languages")
public class Language {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "language_code", nullable = false, unique = true, length = 10)
    private String languageCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "native_name", length = 100)
    private String nativeName;

    @Column(name = "script", length = 50)
    private String script;

    @Column(name = "stt_model_id", length = 100)
    private String sttModelId;

    @Column(name = "tts_engine", length = 50)
    private String ttsEngine;

    @Column(name = "tts_voice_id", length = 100)
    private String ttsVoiceId;

    @Column(name = "spoken_formatter_rules_path")
    private String spokenFormatterRulesPath;

    @Column(name = "llm_quality_tier", nullable = false, length = 20)
    private String llmQualityTier = "good";

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    protected Language() {
    }

    public Language(UUID id, String languageCode, String displayName, String nativeName, String script,
                    String sttModelId, String ttsEngine, String ttsVoiceId, String spokenFormatterRulesPath,
                    String llmQualityTier, boolean enabled) {
        this.id = id;
        this.languageCode = languageCode;
        this.displayName = displayName;
        this.nativeName = nativeName;
        this.script = script;
        this.sttModelId = sttModelId;
        this.ttsEngine = ttsEngine;
        this.ttsVoiceId = ttsVoiceId;
        this.spokenFormatterRulesPath = spokenFormatterRulesPath;
        this.llmQualityTier = llmQualityTier;
        this.enabled = enabled;
        this.addedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getLanguageCode() { return languageCode; }
    public String getDisplayName() { return displayName; }
    public String getNativeName() { return nativeName; }
    public String getScript() { return script; }
    public String getSttModelId() { return sttModelId; }
    public String getTtsEngine() { return ttsEngine; }
    public String getTtsVoiceId() { return ttsVoiceId; }
    public String getSpokenFormatterRulesPath() { return spokenFormatterRulesPath; }
    public String getLlmQualityTier() { return llmQualityTier; }
    public boolean isEnabled() { return enabled; }
    public Instant getAddedAt() { return addedAt; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setTtsEngine(String ttsEngine) { this.ttsEngine = ttsEngine; }
    public void setTtsVoiceId(String ttsVoiceId) { this.ttsVoiceId = ttsVoiceId; }
    public void setLlmQualityTier(String tier) { this.llmQualityTier = tier; }
}
