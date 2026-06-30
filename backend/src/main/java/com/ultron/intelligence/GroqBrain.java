package com.ultron.intelligence;

import com.ultron.config.UltronProperties;
import org.springframework.stereotype.Component;

/** Groq — fastest free inference (LLaMA on custom chips). Opt-in via GROQ_API_KEY. */
@Component
public class GroqBrain extends OpenAiCompatibleBrain {

    public GroqBrain(UltronProperties properties) {
        super("groq", "https://api.groq.com/openai/v1", properties.getBrain());
    }

    @Override
    protected String apiKey() {
        return config.getGroqApiKey();
    }

    @Override
    public String model() {
        return config.getGroqModel();
    }
}
