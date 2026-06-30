package com.ultron.intelligence.rag;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Skill-aware retrieval (L3). Wraps {@link RagService} and boosts items that came from learned
 * skills, on the premise that an explicitly-taught skill is usually more authoritative than an
 * incidental memory for "how do I…" questions. The boost is multiplicative and capped so a weak
 * skill match never outranks a strong memory match outright.
 */
@Component
public class SkillAwareRetriever {

    /** Multiplier applied to skill-sourced item scores. */
    private static final double SKILL_BOOST = 1.35;

    private final RagService rag;

    public SkillAwareRetriever(RagService rag) {
        this.rag = rag;
    }

    /** Retrieve top-k, re-ranked with a skill-domain boost. */
    public List<RetrievedItem> retrieve(String query, int topK) {
        // Pull a wider candidate set so boosting can promote skills that just missed the cut.
        int widen = Math.max(topK, topK * 3);
        List<RetrievedItem> base = rag.retrieve(query, widen);

        return base.stream()
            .map(this::boost)
            .sorted(Comparator.comparingDouble(RetrievedItem::score).reversed())
            .limit(topK > 0 ? topK : 5)
            .toList();
    }

    private RetrievedItem boost(RetrievedItem item) {
        if ("skill".equals(item.kind())) {
            return new RetrievedItem(item.kind(), item.id(), item.title(), item.content(),
                item.score() * SKILL_BOOST);
        }
        return item;
    }
}
