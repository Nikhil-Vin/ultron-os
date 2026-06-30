package com.ultron.intelligence;

import com.ultron.config.UltronProperties;
import org.springframework.stereotype.Component;

/** OpenRouter — routes to free ($0/token) models. Opt-in via OPENROUTER_API_KEY. */
@Component
public class OpenRouterBrain extends OpenAiCompatibleBrain {

    public OpenRouterBrain(UltronProperties properties) {
        super("openrouter", "https://openrouter.ai/api/v1", properties.getBrain());
    }

    @Override
    protected String apiKey() {
        return config.getOpenrouterApiKey();
    }

    @Override
    public String model() {
        return config.getOpenrouterModel();
    }
}
