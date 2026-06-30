package com.ultron.intelligence.language;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Auto language detection (Section 9.10) — runs on every input so Ultron switches language instantly
 * with no command. Devanagari script → Hindi/Marathi (disambiguated by common-word markers); Latin →
 * English (Hinglish is treated as English-base; the model still mixes naturally). Lightweight + fast.
 */
@Component
public class LanguageDetector {

    private static final Pattern DEVANAGARI = Pattern.compile("[\\u0900-\\u097F]");
    // Marathi-distinctive tokens vs Hindi-distinctive tokens.
    private static final String[] MARATHI = {"आहे", "नाही", "मराठी", "तू", "काय", "कसा", "मला", "तुला", "होईल", "का"};
    private static final String[] HINDI = {"है", "हूँ", "हूं", "क्या", "नहीं", "हिंदी", "कर", "रहा", "मुझे", "तुम"};

    /** Returns {@code en}, {@code hi}, or {@code mr}. */
    public String detect(String text) {
        if (text == null || text.isBlank() || !DEVANAGARI.matcher(text).find()) {
            return "en";
        }
        int mr = count(text, MARATHI);
        int hi = count(text, HINDI);
        if (mr > hi) {
            return "mr";
        }
        return "hi"; // default Devanagari → Hindi
    }

    private static int count(String text, String[] markers) {
        int n = 0;
        for (String m : markers) {
            if (text.contains(m)) {
                n++;
            }
        }
        return n;
    }
}
