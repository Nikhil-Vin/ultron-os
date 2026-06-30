package com.ultron.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Strongly-typed binding of all {@code ultron.*} configuration.
 *
 * <p>Defaults are deliberately safe and offline: {@link #autoApprove()} is {@code false}
 * (human-in-the-loop, Section 14) and the GitHub connector runs in {@code fixture} mode.
 */
@ConfigurationProperties(prefix = "ultron")
public class UltronProperties {

    /** Section 14 governance switch. Hardcoded safe default: false (never auto-approve). */
    private boolean autoApprove = false;

    @NestedConfigurationProperty
    private Brain brain = new Brain();

    @NestedConfigurationProperty
    private Github github = new Github();

    @NestedConfigurationProperty
    private Cors cors = new Cors();

    public boolean isAutoApprove() {
        return autoApprove;
    }

    public void setAutoApprove(boolean autoApprove) {
        this.autoApprove = autoApprove;
    }

    public Brain getBrain() {
        return brain;
    }

    public void setBrain(Brain brain) {
        this.brain = brain;
    }

    public Github getGithub() {
        return github;
    }

    public void setGithub(Github github) {
        this.github = github;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    /** Model-agnostic brain settings. Local-first: Ollama is opt-in and degrades to heuristic. */
    public static class Brain {
        private String ollamaBaseUrl = "http://localhost:11434";
        private String ollamaModel = "llama3.1";
        private boolean ollamaEnabled = true;

        public String getOllamaBaseUrl() {
            return ollamaBaseUrl;
        }

        public void setOllamaBaseUrl(String ollamaBaseUrl) {
            this.ollamaBaseUrl = ollamaBaseUrl;
        }

        public String getOllamaModel() {
            return ollamaModel;
        }

        public void setOllamaModel(String ollamaModel) {
            this.ollamaModel = ollamaModel;
        }

        public boolean isOllamaEnabled() {
            return ollamaEnabled;
        }

        public void setOllamaEnabled(boolean ollamaEnabled) {
            this.ollamaEnabled = ollamaEnabled;
        }
    }

    /** GitHub connector mode: {@code fixture} (offline, default) or {@code rest} (opt-in). */
    public static class Github {
        private String mode = "fixture";
        private String token = "";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    /** CORS allow-list for the React frontend. No wildcard origins. */
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
