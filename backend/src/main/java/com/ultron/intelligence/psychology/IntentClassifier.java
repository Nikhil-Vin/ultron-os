package com.ultron.intelligence.psychology;

import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Intent classification (L7 — The Mind). Phase 1 is a deterministic, offline heuristic that maps
 * an utterance to a coarse {@link Intent}; the Python psychology layer (HuggingFace + LoRA) can
 * replace it later behind the same surface. The kernel uses this to route the agent loop.
 */
@Component
public class IntentClassifier {

    /** Coarse intent buckets that drive agent-loop routing + risk. */
    public enum Intent {
        CAPTURE,     // "remember / note / save this"
        PLAN,        // "plan / prioritise my day"
        TRADE_WATCH, // "watch the market / show my watchlist"
        TRADE_LIVE,  // "buy / sell / place a live trade"
        QUESTION,    // information request → RAG
        SMALL_TALK   // greetings / chit-chat
    }

    public Intent classify(String text) {
        if (text == null || text.isBlank()) {
            return Intent.QUESTION;
        }
        String t = text.toLowerCase(Locale.ROOT).strip();

        if (containsAny(t, "live trade", "live-trade", "place an order", "execute trade")
            || startsWithAny(t, "buy ", "sell ")
            || containsAny(t, " buy ", " sell ")) {
            return Intent.TRADE_LIVE;
        }
        if (containsAny(t, "watchlist", "market", "watch the", "stock price", "ticker")) {
            return Intent.TRADE_WATCH;
        }
        if (containsAny(t, "remember", "note that", "make a note", "capture", "save this", "log that")) {
            return Intent.CAPTURE;
        }
        if (containsAny(t, "plan ", "plan my", "prioriti", "schedule my", "organise", "organize", "to-do", "todo")) {
            return Intent.PLAN;
        }
        if (isSmallTalk(t)) {
            return Intent.SMALL_TALK;
        }
        return Intent.QUESTION;
    }

    private static boolean isSmallTalk(String t) {
        String[] greetings = {"hi", "hello", "hey", "good morning", "good evening", "how are you", "thanks", "thank you"};
        for (String g : greetings) {
            if (t.equals(g) || t.startsWith(g + " ") || t.startsWith(g + ",")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String t, String... needles) {
        for (String n : needles) {
            if (t.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithAny(String t, String... prefixes) {
        for (String p : prefixes) {
            if (t.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
