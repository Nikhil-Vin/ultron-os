-- =============================================================================
-- V4__languages.sql — Ultron-OS Phase 2: multilingual registry (Section 9.10)
--   The source of truth for which languages Ultron speaks. Adding a language is a
--   registry entry, not a pipeline change. Defaults (en/hi/mr) are also seeded by
--   LanguageRegistry on startup so the in-memory H2 test profile has them too.
-- =============================================================================

CREATE TABLE IF NOT EXISTS supported_languages (
    id                          UUID PRIMARY KEY,
    language_code               VARCHAR(10) UNIQUE NOT NULL,   -- ISO 639-1: en, hi, mr, ta
    display_name                VARCHAR(100) NOT NULL,         -- Hindi, Marathi, English
    native_name                 VARCHAR(100),                  -- हिन्दी, मराठी
    script                      VARCHAR(50),                   -- Devanagari, Latin
    stt_model_id                VARCHAR(100),                  -- whisper checkpoint/config
    tts_engine                  VARCHAR(50),                   -- piper | elevenlabs | azure | coqui_clone
    tts_voice_id                VARCHAR(100),                  -- engine-specific voice id
    spoken_formatter_rules_path TEXT,                          -- per-language speech rules
    llm_quality_tier            VARCHAR(20) NOT NULL DEFAULT 'good', -- native | good | experimental
    enabled                     BOOLEAN NOT NULL DEFAULT TRUE,
    added_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_languages_enabled ON supported_languages (enabled);
