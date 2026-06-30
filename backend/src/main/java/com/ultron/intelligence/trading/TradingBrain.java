package com.ultron.intelligence.trading;

import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.rag.RagService;
import com.ultron.intelligence.rag.RetrievedItem;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Trading brain (Section 7) — the specialist reasoning chain for markets. Fuses a structured
 * {@link SignalGenerator} signal, the owner's active {@link TradingRule}s, ingested trading
 * knowledge (RAG), and the live brain into a single piece of reasoned, rule-grounded advice.
 * READ-level: it advises, it never executes.
 */
@Service
public class TradingBrain {

    private final SignalGenerator signalGenerator;
    private final TradingRuleRepository rules;
    private final RagService rag;
    private final BrainSelector brain;

    public TradingBrain(SignalGenerator signalGenerator, TradingRuleRepository rules,
                        RagService rag, BrainSelector brain) {
        this.signalGenerator = signalGenerator;
        this.rules = rules;
        this.rag = rag;
        this.brain = brain;
    }

    /**
     * Produce advice for an instrument given an indicator snapshot.
     *
     * @return the generated signal + the brain's rule-grounded narrative
     */
    public Advice advise(String instrument, Map<String, Double> indicators) {
        TradingSignal signal = signalGenerator.generate(instrument, indicators);

        List<TradingRule> active = rules.findByActiveTrueOrderByCreatedAtDesc();
        String ruleText = active.isEmpty()
            ? "(no personal trading rules defined yet)"
            : active.stream().map(r -> "- " + r.getRuleName() + ": " + r.getRuleText())
                .collect(Collectors.joining("\n"));

        List<RetrievedItem> knowledge = rag.retrieve(
            "trading setup " + instrument + " " + signal.getSignalType() + " " + signal.getReasoning(), 3);
        String knowledgeText = knowledge.isEmpty()
            ? "(no ingested trading knowledge retrieved)"
            : knowledge.stream().map(k -> "- " + k.content()).collect(Collectors.joining("\n"));

        String prompt = """
            You are Ultron's trading desk. Give a brief, disciplined read. Do NOT invent prices.
            Signal: %s on %s (confidence %.2f). Reasoning: %s

            My rules:
            %s

            Relevant ingested knowledge:
            %s

            In 3-4 sentences: what's the read, does it fit my rules, and what's the single most
            important caution? End with the grade (A/B/C setup).
            """.formatted(signal.getSignalType(), instrument,
                signal.getConfidence().doubleValue(), signal.getReasoning(), ruleText, knowledgeText);

        String narrative = brain.think(prompt);
        return new Advice(signal, narrative, active.size(), knowledge.size());
    }

    /**
     * @param signal       the structured signal
     * @param narrative    the rule-grounded reasoning (LLM or heuristic)
     * @param rulesApplied number of active rules considered
     * @param knowledgeUsed number of retrieved knowledge chunks
     */
    public record Advice(TradingSignal signal, String narrative, int rulesApplied, int knowledgeUsed) {
    }
}
