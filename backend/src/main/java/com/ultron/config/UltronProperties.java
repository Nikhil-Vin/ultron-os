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

    /** Optional shared API key. When set, /api/** requires the X-Ultron-Key header. Blank = open. */
    private String apiKey = "";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @NestedConfigurationProperty
    private Brain brain = new Brain();

    @NestedConfigurationProperty
    private Github github = new Github();

    @NestedConfigurationProperty
    private Cors cors = new Cors();

    @NestedConfigurationProperty
    private Voice voice = new Voice();

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

    public Voice getVoice() {
        return voice;
    }

    public void setVoice(Voice voice) {
        this.voice = voice;
    }

    /** Model-agnostic brain settings. Local-first: Ollama is opt-in and degrades to heuristic. */
    public static class Brain {
        private String ollamaBaseUrl = "http://localhost:11434";
        private String ollamaModel = "llama3.1";
        /** Dedicated embedding model — Ollama chat models (e.g. llama3.1) cannot embed. */
        private String ollamaEmbeddingModel = "nomic-embed-text";
        private boolean ollamaEnabled = true;

        /** Ultron's persona / system prompt, prepended to every reasoning call. No secrets. */
        private String systemPrompt =
            "You are Ultron, a local-first personal AI that serves one owner. "
                + "Be concise, direct, and practical. Use only the context you are given; "
                + "if you are unsure, say so plainly rather than inventing facts.";
        /** Sampling temperature (0 = deterministic, higher = more creative). */
        private double temperature = 0.2;
        /** How long Ollama keeps the model loaded after a call (e.g. 5m, 0 to unload). */
        private String keepAlive = "5m";
        /** TTL in millis for caching the availability probe so reasoning never re-probes per call. */
        private long availabilityCacheMillis = 5_000;

        // --- Cloud LLM upgrades (opt-in; blank keys → unused). Preferred over Ollama when set. ---
        private String openaiApiKey = "";
        private String openaiModel = "gpt-4o-mini";
        private String openaiBaseUrl = "https://api.openai.com/v1";
        private String geminiApiKey = "";
        private String geminiModel = "gemini-1.5-flash";

        public String getOpenaiApiKey() { return openaiApiKey; }
        public void setOpenaiApiKey(String v) { this.openaiApiKey = v; }
        public String getOpenaiModel() { return openaiModel; }
        public void setOpenaiModel(String v) { this.openaiModel = v; }
        public String getOpenaiBaseUrl() { return openaiBaseUrl; }
        public void setOpenaiBaseUrl(String v) { this.openaiBaseUrl = v; }
        public String getGeminiApiKey() { return geminiApiKey; }
        public void setGeminiApiKey(String v) { this.geminiApiKey = v; }
        public String getGeminiModel() { return geminiModel; }
        public void setGeminiModel(String v) { this.geminiModel = v; }

        // Additional opt-in providers (all free-tier capable).
        private String anthropicApiKey = "";
        private String anthropicModel = "claude-3-haiku-20240307";
        private String groqApiKey = "";
        private String groqModel = "llama-3.1-70b-versatile";
        private String openrouterApiKey = "";
        private String openrouterModel = "meta-llama/llama-3.1-8b-instruct:free";
        private String githubModelsModel = "gpt-4o";   // uses the existing GitHub token
        private String nvidiaApiKey = "";
        private String nvidiaModel = "deepseek-ai/deepseek-r1";

        public String getAnthropicApiKey() { return anthropicApiKey; }
        public void setAnthropicApiKey(String v) { this.anthropicApiKey = v; }
        public String getAnthropicModel() { return anthropicModel; }
        public void setAnthropicModel(String v) { this.anthropicModel = v; }
        public String getGroqApiKey() { return groqApiKey; }
        public void setGroqApiKey(String v) { this.groqApiKey = v; }
        public String getGroqModel() { return groqModel; }
        public void setGroqModel(String v) { this.groqModel = v; }
        public String getOpenrouterApiKey() { return openrouterApiKey; }
        public void setOpenrouterApiKey(String v) { this.openrouterApiKey = v; }
        public String getOpenrouterModel() { return openrouterModel; }
        public void setOpenrouterModel(String v) { this.openrouterModel = v; }
        public String getGithubModelsModel() { return githubModelsModel; }
        public void setGithubModelsModel(String v) { this.githubModelsModel = v; }
        public String getNvidiaApiKey() { return nvidiaApiKey; }
        public void setNvidiaApiKey(String v) { this.nvidiaApiKey = v; }
        public String getNvidiaModel() { return nvidiaModel; }
        public void setNvidiaModel(String v) { this.nvidiaModel = v; }

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

        public String getOllamaEmbeddingModel() {
            return ollamaEmbeddingModel;
        }

        public void setOllamaEmbeddingModel(String ollamaEmbeddingModel) {
            this.ollamaEmbeddingModel = ollamaEmbeddingModel;
        }

        public boolean isOllamaEnabled() {
            return ollamaEnabled;
        }

        public void setOllamaEnabled(boolean ollamaEnabled) {
            this.ollamaEnabled = ollamaEnabled;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public String getKeepAlive() {
            return keepAlive;
        }

        public void setKeepAlive(String keepAlive) {
            this.keepAlive = keepAlive;
        }

        public long getAvailabilityCacheMillis() {
            return availabilityCacheMillis;
        }

        public void setAvailabilityCacheMillis(long availabilityCacheMillis) {
            this.availabilityCacheMillis = availabilityCacheMillis;
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
        private String allowedOrigins = "http://localhost:5173,http://localhost:3000";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    /** Voice settings (Section 9.9 / 14): default profile + biometric threshold for CRITICAL. */
    public static class Voice {
        /** Active TTS voice profile id (default | warm | sharp | premium | cloud-hd | clone). */
        private String defaultProfile = "default";
        /** Resemblyzer cosine-similarity threshold a speaker must meet to pass voice ID. */
        private double biometricThreshold = 0.75;
        /** When false, voice biometric is not enrolled yet → CRITICAL voice actions stay blocked. */
        private boolean biometricEnrolled = false;

        public String getDefaultProfile() {
            return defaultProfile;
        }

        public void setDefaultProfile(String defaultProfile) {
            this.defaultProfile = defaultProfile;
        }

        public double getBiometricThreshold() {
            return biometricThreshold;
        }

        public void setBiometricThreshold(double biometricThreshold) {
            this.biometricThreshold = biometricThreshold;
        }

        public boolean isBiometricEnrolled() {
            return biometricEnrolled;
        }

        public void setBiometricEnrolled(boolean biometricEnrolled) {
            this.biometricEnrolled = biometricEnrolled;
        }
    }
}
