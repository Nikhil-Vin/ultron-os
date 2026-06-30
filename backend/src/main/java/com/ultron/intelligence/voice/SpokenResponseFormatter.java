package com.ultron.intelligence.voice;

import com.ultron.kernel.WorkModeManager;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Spoken-response formatter (L3 — Section 9.9.B). Converts written/markdown LLM output into natural
 * speech before it reaches TTS: strips headers, bullets, bold/italic markers, code fences, and
 * links, rewrites list items into flowing phrasing ("first… second… and third…"), and applies the
 * active {@link WorkModeManager.WorkMode} tone (terse modes clip to the first sentences).
 */
@Component
public class SpokenResponseFormatter {

    private static final Pattern CODE_FENCE = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern HEADER = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s*");
    private static final Pattern BOLD_ITALIC = Pattern.compile("(\\*\\*|\\*|__|_)(.*?)\\1");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");
    private static final Pattern BULLET = Pattern.compile("(?m)^\\s*[-*+]\\s+");
    private static final Pattern NUMBERED = Pattern.compile("(?m)^\\s*\\d+[.)]\\s+");
    private static final Pattern MULTISPACE = Pattern.compile("[ \\t]{2,}");
    private static final String[] ORDINALS =
        {"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth"};

    private final WorkModeManager workModeManager;

    public SpokenResponseFormatter(WorkModeManager workModeManager) {
        this.workModeManager = workModeManager;
    }

    /** Format using the current work mode's tone. */
    public String format(String text) {
        return format(text, workModeManager.current());
    }

    /** Format for speech under a specific work mode. */
    public String format(String text, WorkModeManager.WorkMode mode) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String s = text;

        // Drop code blocks entirely (unspeakable); keep inline code as its bare token.
        s = CODE_FENCE.matcher(s).replaceAll(" ");
        s = INLINE_CODE.matcher(s).replaceAll("$1");

        // Links → just the visible text.
        s = LINK.matcher(s).replaceAll("$1");

        // Headers, emphasis markers.
        s = HEADER.matcher(s).replaceAll("");
        s = BOLD_ITALIC.matcher(s).replaceAll("$2");

        // Rewrite list items into spoken sequence markers.
        s = rewriteLists(s);

        // Collapse whitespace/newlines into speakable flow.
        s = s.replaceAll("\\r", "");
        s = MULTISPACE.matcher(s).replaceAll(" ");
        s = s.replaceAll("\\n{2,}", ". ").replaceAll("\\n", ". ");
        s = s.replaceAll("\\.\\s*\\.", ".").replaceAll("\\s+\\.", ".").trim();

        // Apply work-mode tone: terse modes speak only the lead sentences.
        WorkModeManager.Behavior b = mode.behavior();
        if (b.terse()) {
            s = clipToSentences(s, 2);
        }
        return s;
    }

    private String rewriteLists(String text) {
        StringBuilder out = new StringBuilder();
        int ordinal = 0;
        for (String line : text.split("\n")) {
            String trimmed = line.strip();
            boolean isNumbered = NUMBERED.matcher(trimmed).find();
            boolean isBullet = BULLET.matcher(trimmed).find();
            if (isNumbered || isBullet) {
                String body = NUMBERED.matcher(trimmed).replaceFirst("");
                body = BULLET.matcher(body).replaceFirst("");
                String marker = ordinal < ORDINALS.length ? ORDINALS[ordinal] : "next";
                out.append(ordinal == 0 ? "" : " ").append(marker).append(", ").append(body).append(";");
                ordinal++;
            } else {
                out.append("\n").append(line);
            }
        }
        return out.toString();
    }

    private static String clipToSentences(String text, int maxSentences) {
        String[] parts = text.split("(?<=[.!?])\\s+");
        if (parts.length <= maxSentences) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxSentences; i++) {
            sb.append(parts[i]);
            if (!parts[i].endsWith(".") && !parts[i].endsWith("!") && !parts[i].endsWith("?")) {
                sb.append('.');
            }
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
