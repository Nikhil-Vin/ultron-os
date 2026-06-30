package com.ultron.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Connection settings for the optional Python {@code ai-layer} (FastAPI) that hosts the heavy
 * deep-learning services (Sentence-Transformers, Faiss, Whisper, LoRA, etc.).
 *
 * <p>Disabled by default: the Java backend is fully functional offline using its heuristic brain
 * and embedder. When {@code ultron.python-bridge.enabled=true} and a base URL is set, callers may
 * delegate embedding / ingestion / psychology to the Python service. The {@code apiKey} is a
 * shared secret for the local hop and is never logged or echoed.
 */
@Component
@ConfigurationProperties(prefix = "ultron.python-bridge")
public class PythonBridgeConfig {

    private boolean enabled = false;
    private String baseUrl = "http://localhost:8000";
    private String apiKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /** True only when explicitly enabled AND a base URL is configured. */
    public boolean isUsable() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }
}
