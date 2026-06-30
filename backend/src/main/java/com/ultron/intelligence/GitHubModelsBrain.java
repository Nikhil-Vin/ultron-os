package com.ultron.intelligence;

import com.ultron.config.UltronProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub Models (Azure AI inference) — free for developers, reuses the existing GITHUB_TOKEN.
 * Opt-in: available whenever {@code ultron.github.token} is set.
 */
@Component
public class GitHubModelsBrain extends OpenAiCompatibleBrain {

    private final UltronProperties properties;

    public GitHubModelsBrain(UltronProperties properties) {
        super("github-models", "https://models.inference.ai.azure.com", properties.getBrain());
        this.properties = properties;
    }

    @Override
    protected String apiKey() {
        return properties.getGithub().getToken();
    }

    @Override
    public String model() {
        return config.getGithubModelsModel();
    }
}
