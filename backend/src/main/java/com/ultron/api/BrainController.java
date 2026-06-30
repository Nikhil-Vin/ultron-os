package com.ultron.api;

import com.ultron.intelligence.BrainSelector;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/brain/providers} — lists every reasoning provider (OpenAI, Anthropic, Gemini,
 * Groq, GitHub Models, OpenRouter, NVIDIA, Ollama, Heuristic) with its model, whether it's
 * configured/available, and which one is currently active. No secrets exposed.
 */
@RestController
@RequestMapping("/api/brain")
public class BrainController {

    private final BrainSelector brain;

    public BrainController(BrainSelector brain) {
        this.brain = brain;
    }

    @GetMapping("/providers")
    public Map<String, Object> providers() {
        List<BrainSelector.Provider> providers = brain.providers();
        BrainSelector.Status status = brain.status();
        return Map.of(
            "active", status.active(),
            "activeModel", status.model(),
            "llmActive", status.llmActive(),
            "pinned", brain.pinned(),
            "providers", providers);
    }

    /** Pin a preferred provider at runtime (or {@code auto} to clear). */
    @PostMapping("/select")
    public Map<String, Object> select(@RequestBody SelectRequest req) {
        brain.pin(req.provider());
        return providers();
    }

    public record SelectRequest(String provider) {
    }
}
