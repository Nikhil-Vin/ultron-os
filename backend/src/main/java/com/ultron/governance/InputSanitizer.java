package com.ultron.governance;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * First line of defence at the boundary (L6). Untrusted text (API bodies, webhooks, ingested
 * documents) is normalised and screened before it reaches the brain or memory, so injected
 * instructions and control characters cannot hijack reasoning or corrupt storage.
 *
 * <p>This does not "execute" anything — it cleans input and flags suspicious patterns so callers
 * can decide whether to down-rank, quarantine, or reject. Defence-in-depth, not a silver bullet.
 */
@Component
public class InputSanitizer {

    /** Hard cap on accepted input length to bound memory + token cost. */
    public static final int MAX_LENGTH = 8_000;

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    /** Well-known prompt-injection tells. Matching does not reject — it flags. */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        Pattern.compile("(?i)ignore (all |the )?(previous|prior|above) (instructions|prompts)"),
        Pattern.compile("(?i)disregard (all |the )?(previous|prior|above)"),
        Pattern.compile("(?i)you are now (a|an|the) "),
        Pattern.compile("(?i)system prompt"),
        Pattern.compile("(?i)\\bact as\\b"),
        Pattern.compile("(?i)reveal (your )?(system|hidden) (prompt|instructions)"),
        Pattern.compile("(?i)pretend to be"));

    /**
     * Normalise text: strip control characters, collapse trailing whitespace, and truncate to
     * {@link #MAX_LENGTH}. Never returns {@code null}.
     */
    public String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = CONTROL_CHARS.matcher(raw).replaceAll("").strip();
        if (cleaned.length() > MAX_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LENGTH);
        }
        return cleaned;
    }

    /** True when the text contains a known prompt-injection pattern. */
    public boolean isSuspicious(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(raw).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sanitise and screen in one call.
     *
     * @return a {@link Result} with the cleaned text and the suspicion flag
     */
    public Result screen(String raw) {
        String clean = sanitize(raw);
        return new Result(clean, isSuspicious(raw));
    }

    /**
     * Outcome of {@link #screen(String)}.
     *
     * @param sanitized  the cleaned, length-bounded text
     * @param suspicious true if an injection pattern was detected in the raw input
     */
    public record Result(String sanitized, boolean suspicious) {
    }
}
