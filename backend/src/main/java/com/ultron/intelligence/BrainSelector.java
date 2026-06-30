package com.ultron.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Chooses the active {@link Brain} at call time (L3). Default priority (first available wins):
 * OpenAI → Anthropic → Gemini → Groq → GitHub Models → OpenRouter → NVIDIA → Ollama → Heuristic.
 * The owner can pin a preferred provider at runtime via {@link #pin(String)} (used by
 * {@code /api/brain/select}); if the pinned provider is unavailable, it falls back to auto.
 * Fail-safe: there is always a working brain.
 */
@Component
public class BrainSelector {

    private static final Logger log = LoggerFactory.getLogger(BrainSelector.class);

    private static final List<String> ORDER =
        List.of("openai", "anthropic", "gemini", "groq", "github-models", "openrouter", "nvidia");

    private final OllamaBrain ollamaBrain;
    private final HeuristicBrain heuristicBrain;

    @Autowired(required = false)
    private List<Brain> allBrains = List.of();

    /** Active response language (optional; injected by Spring). Tests run without it (English). */
    @Autowired(required = false)
    private com.ultron.intelligence.language.LanguagePreference languagePreference;

    /** Owner-pinned provider, or null for automatic priority. */
    private volatile String pinned = null;

    public BrainSelector(OllamaBrain ollamaBrain, HeuristicBrain heuristicBrain) {
        this.ollamaBrain = ollamaBrain;
        this.heuristicBrain = heuristicBrain;
    }

    /** Pin a provider by name ({@code openai}…{@code heuristic}); {@code auto}/blank clears it. */
    public void pin(String provider) {
        if (provider == null || provider.isBlank() || "auto".equalsIgnoreCase(provider)) {
            pinned = null;
            log.info("Brain provider pin cleared → auto");
        } else {
            pinned = provider.toLowerCase();
            log.info("Brain provider pinned → {}", pinned);
        }
    }

    public String pinned() {
        return pinned == null ? "auto" : pinned;
    }

    private Brain byName(String name) {
        if ("ollama".equals(name)) return ollamaBrain;
        if ("heuristic".equals(name)) return heuristicBrain;
        return find(name);
    }

    private Brain find(String name) {
        if (allBrains == null) return null;
        for (Brain b : allBrains) {
            if (b != ollamaBrain && b != heuristicBrain && b.name().equals(name)) return b;
        }
        return null;
    }

    private Brain availableCloudBrain() {
        for (String wanted : ORDER) {
            Brain b = find(wanted);
            if (b != null && b.isAvailable()) return b;
        }
        return null;
    }

    /** The brain that will actually serve right now (honours a runtime pin). */
    public Brain active() {
        if (pinned != null) {
            Brain p = byName(pinned);
            if (p != null && p.isAvailable()) return p;
        }
        Brain cloud = availableCloudBrain();
        if (cloud != null) return cloud;
        if (ollamaBrain.isAvailable()) return ollamaBrain;
        return heuristicBrain;
    }

    /** Reason via the active brain, falling back to the heuristic on a blank live answer. */
    public String think(String prompt) {
        String p = languagePreference == null ? prompt : languagePreference.applyTo(prompt);
        Brain brain = active();
        String answer = brain.think(p);
        if ((answer == null || answer.isBlank()) && brain != heuristicBrain) {
            return heuristicBrain.think(p);
        }
        return answer;
    }

    /** Stream tokens via the active brain (Ollama + all cloud brains stream natively). */
    public String streamThink(String prompt, Consumer<String> onToken) {
        String p = languagePreference == null ? prompt : languagePreference.applyTo(prompt);
        Brain brain = active();
        if (brain == heuristicBrain) {
            String a = heuristicBrain.think(p);
            if (a != null && !a.isBlank()) onToken.accept(a);
            return a;
        }
        String streamed = brain.streamThink(p, onToken);
        if (streamed == null || streamed.isBlank()) {
            String h = heuristicBrain.think(p);
            if (h != null && !h.isBlank()) onToken.accept(h);
            return h;
        }
        return streamed;
    }

    /** Health-facing status (no secrets). */
    public Status status() {
        Brain b = active();
        if (b == heuristicBrain) return new Status("heuristic", "n/a", false);
        if (b == ollamaBrain) return new Status("ollama", ollamaBrain.model(), ollamaBrain.isAvailable());
        return new Status(b.name(), modelOf(b), true);
    }

    /** All known providers with availability + active flag (for {@code /api/brain/providers}). */
    public List<Provider> providers() {
        List<Provider> out = new ArrayList<>();
        Brain activeBrain = active();
        for (String name : ORDER) {
            Brain b = find(name);
            if (b != null) out.add(new Provider(name, modelOf(b), b.isAvailable(), b == activeBrain));
        }
        out.add(new Provider("ollama", ollamaBrain.model(), ollamaBrain.isAvailable(), activeBrain == ollamaBrain));
        out.add(new Provider("heuristic", "n/a", true, activeBrain == heuristicBrain));
        return out;
    }

    private static String modelOf(Brain b) {
        if (b instanceof OpenAiCompatibleBrain c) return c.model();
        if (b instanceof GeminiBrain g) return g.model();
        if (b instanceof AnthropicBrain a) return a.model();
        return b.name();
    }

    /** Reasoning-layer status for {@code /api/health}. */
    public record Status(String active, String model, boolean llmActive) {
    }

    /** A reasoning provider's status. */
    public record Provider(String name, String model, boolean available, boolean active) {
    }
}
