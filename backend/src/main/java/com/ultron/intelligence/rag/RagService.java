package com.ultron.intelligence.rag;

import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.intelligence.embedding.Vectors;
import com.ultron.memory.Memory;
import com.ultron.memory.MemoryService;
import com.ultron.skills.Skill;
import com.ultron.skills.SkillService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Retrieval-Augmented Generation (L3 — The Brain). Embeds a query, ranks the owner's memories and
 * learned skills by cosine similarity, and asks the active {@link com.ultron.intelligence.Brain}
 * to answer grounded only in what was retrieved.
 *
 * <p>Fail-safe by design: when an item has no embedding, or its embedding dimension differs from
 * the query's (e.g. it was embedded by a different model), the item is scored by lexical token
 * overlap instead — so retrieval never silently drops to nothing.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int DEFAULT_TOP_K = 5;

    private final MemoryService memoryService;
    private final SkillService skillService;
    private final EmbedderSelector embedder;
    private final BrainSelector brain;

    public RagService(MemoryService memoryService, SkillService skillService,
                      EmbedderSelector embedder, BrainSelector brain) {
        this.memoryService = memoryService;
        this.skillService = skillService;
        this.embedder = embedder;
        this.brain = brain;
    }

    /** Retrieve the top-k most relevant memories + skills for a query. */
    public List<RetrievedItem> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int k = topK > 0 ? topK : DEFAULT_TOP_K;
        float[] queryVec = embedder.embed(query);
        Set<String> queryTokens = tokenSet(query);

        List<RetrievedItem> scored = new ArrayList<>();

        for (Memory m : memoryService.candidatesForRag()) {
            double score = score(queryVec, queryTokens, m.getEmbedding(), m.getContent(), m.getTags());
            scored.add(new RetrievedItem("memory", m.getId().toString(), m.getType(), m.getContent(), score));
        }
        for (Skill s : skillService.candidatesForRag()) {
            String text = s.getName() + " — " + s.getContent();
            double score = score(queryVec, queryTokens, s.getEmbedding(), text, s.getTags());
            scored.add(new RetrievedItem("skill", s.getId().toString(), s.getName(), text, score));
        }

        return scored.stream()
            .filter(item -> item.score() > 0.0)
            .sorted(Comparator.comparingDouble(RetrievedItem::score).reversed())
            .limit(k)
            .toList();
    }

    /**
     * Answer a question grounded in retrieved memories + skills. Returns the grounded answer plus
     * the items used as context.
     */
    public RagAnswer answer(String question, int topK) {
        List<RetrievedItem> context = retrieve(question, topK);
        String prompt = buildPrompt(question, context);
        String answer = brain.think(prompt);
        log.info("RAG answer for question chars={} usedContext={}", question == null ? 0 : question.length(), context.size());
        return new RagAnswer(answer, context);
    }

    private double score(float[] queryVec, Set<String> queryTokens, String embeddingCsv,
                         String text, String tags) {
        float[] vec = Vectors.fromCsv(embeddingCsv);
        if (vec.length > 0 && vec.length == queryVec.length) {
            return Vectors.cosine(queryVec, vec);
        }
        // Fail-safe: lexical Jaccard overlap when embeddings are missing or incompatible.
        Set<String> docTokens = tokenSet(text + (tags == null ? "" : " " + tags));
        return jaccard(queryTokens, docTokens);
    }

    private static String buildPrompt(String question, List<RetrievedItem> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Answer the question using ONLY the context below. ")
          .append("If the context is insufficient, say so plainly.\n\nContext:\n");
        if (context.isEmpty()) {
            sb.append("(no relevant memories or skills found)\n");
        } else {
            int i = 1;
            for (RetrievedItem item : context) {
                sb.append(i++).append(". [").append(item.kind()).append("] ")
                  .append(item.content()).append('\n');
            }
        }
        sb.append("\nQuestion: ").append(question == null ? "" : question.strip());
        return sb.toString();
    }

    private static Set<String> tokenSet(String text) {
        Set<String> tokens = new TreeSet<>();
        if (text == null) {
            return tokens;
        }
        for (String t : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (t.length() >= 2) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String t : a) {
            if (b.contains(t)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    /**
     * A grounded answer plus the context it was built from.
     *
     * @param answer  the brain's grounded reply
     * @param context the retrieved items used as grounding
     */
    public record RagAnswer(String answer, List<RetrievedItem> context) {
    }
}
