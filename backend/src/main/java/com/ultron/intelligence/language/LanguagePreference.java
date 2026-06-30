package com.ultron.intelligence.language;

import org.springframework.stereotype.Component;

/**
 * The active spoken/response language (L3 — Section 9.10). Set at runtime (voice command or UI), it
 * makes the Brain think + respond in that language. Injected once in {@link
 * com.ultron.intelligence.BrainSelector} so every provider (Ollama + all cloud brains) honours it —
 * the formatter never translates; the model itself responds in-language.
 */
@Component
public class LanguagePreference {

    private volatile String code = "en";

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = (code == null || code.isBlank()) ? "en" : code.toLowerCase();
    }

    /** A strong response-language directive to prepend to a prompt, or empty for English. */
    public String instruction() {
        return switch (code) {
            case "hi" -> "Respond in Hindi (हिन्दी). Use Devanagari script. Match the user's language "
                + "naturally — if they mix Hindi and English, you can too.";
            case "mr" -> "Respond in Marathi (मराठी). Use Devanagari script. Match natural Marathi "
                + "conversation style.";
            default -> "";
        };
    }

    /** Prepend the directive to a prompt (no-op for English). */
    public String applyTo(String prompt) {
        String dir = instruction();
        if (dir.isEmpty()) {
            return prompt;
        }
        return dir + "\n\n" + (prompt == null ? "" : prompt);
    }
}
