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
        return directiveFor(code);
    }

    /** Directive for a specific language code (used for per-input auto-detection). */
    public static String directiveFor(String code) {
        return switch (code == null ? "en" : code.toLowerCase()) {
            case "hi" -> "तू Ultron है। हिंदी में naturally बात कर — Hinglish भी चलेगा। English में सोचकर "
                + "translate मत कर, सीधे हिंदी में सोच और बोल। छोटा, तीखा, दोस्त जैसा।";
            case "mr" -> "तू Ultron आहेस। मराठीत naturally बोल — मित्रासारखा, textbook सारखा नाही। "
                + "इंग्रजीतून भाषांतर करू नकोस, थेट मराठीत विचार कर आणि बोल।";
            default -> "";
        };
    }

    /** Prepend the directive to a prompt (no-op for English). */
    public String applyTo(String prompt) {
        return applyToCode(prompt, code);
    }

    /** Prepend the directive for an explicit code (per-input). */
    public static String applyToCode(String prompt, String code) {
        String dir = directiveFor(code);
        if (dir.isEmpty()) {
            return prompt;
        }
        return dir + "\n\n" + (prompt == null ? "" : prompt);
    }
}
