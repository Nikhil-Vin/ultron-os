package com.ultron.intelligence;

import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.stereotype.Component;

/**
 * Rolling conversation memory (Section 4 — keep context). Holds the last N spoken exchanges and
 * renders them as context injected before each chat Brain call, so Ultron understands references
 * ("the one you mentioned", "pehle wala") and stays coherent across turns — like a real person.
 */
@Component
public class ConversationMemory {

    private static final int MAX = 10;
    private final Deque<String[]> turns = new ArrayDeque<>(); // [user, assistant]

    public synchronized void record(String user, String assistant) {
        if (user == null || user.isBlank()) {
            return;
        }
        turns.addLast(new String[]{user.strip(), assistant == null ? "" : assistant.strip()});
        while (turns.size() > MAX) {
            turns.removeFirst();
        }
    }

    /** Recent exchanges as a compact context block, or empty when there's no history. */
    public synchronized String context() {
        if (turns.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Recent conversation (most recent last):\n");
        for (String[] t : turns) {
            sb.append("Owner: ").append(trim(t[0])).append('\n');
            if (!t[1].isEmpty()) {
                sb.append("Ultron: ").append(trim(t[1])).append('\n');
            }
        }
        return sb.toString();
    }

    private static String trim(String s) {
        return s.length() > 240 ? s.substring(0, 240) + "…" : s;
    }
}
