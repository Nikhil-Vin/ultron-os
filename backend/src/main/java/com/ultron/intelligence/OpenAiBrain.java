package com.ultron.intelligence;

import com.ultron.config.UltronProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI cloud brain (L3) — opt-in upgrade behind the model-agnostic {@link Brain} surface.
 * Available only when {@code ultron.brain.openai-api-key} is set. Streams via the shared
 * OpenAI-compatible base. Key from env, never hard-coded (Section 3 moat).
 */
@Component
public class OpenAiBrain extends OpenAiCompatibleBrain {

    public OpenAiBrain(UltronProperties properties) {
        super("openai", properties.getBrain().getOpenaiBaseUrl(), properties.getBrain());
    }

    @Override
    protected String apiKey() {
        return config.getOpenaiApiKey();
    }

    @Override
    public String model() {
        return config.getOpenaiModel();
    }
}
