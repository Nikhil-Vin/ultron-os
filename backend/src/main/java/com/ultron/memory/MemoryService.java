package com.ultron.memory;

import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.intelligence.embedding.Vectors;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Memory operations (L4). Saving a memory is a {@code LOW} risk action, so it flows through
 * the {@link ApprovalGate} (auto-approved + audited). Recall is a {@code READ} action.
 *
 * <p>Phase 1: every saved memory is embedded (via {@link EmbedderSelector}) so the RAG layer
 * can recall it semantically; keyword recall remains the fail-safe fallback.
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
    private static final int DEFAULT_LIMIT = 25;
    /** Upper bound of recent memories scanned for in-Java semantic ranking. */
    private static final int RAG_CANDIDATE_WINDOW = 500;

    private final MemoryRepository repository;
    private final ApprovalGate approvalGate;
    private final EmbedderSelector embedder;

    public MemoryService(MemoryRepository repository, ApprovalGate approvalGate, EmbedderSelector embedder) {
        this.repository = repository;
        this.approvalGate = approvalGate;
        this.embedder = embedder;
    }

    /**
     * Persist a new memory. Passes through the approval gate (LOW risk → auto + audited) and is
     * embedded for semantic recall.
     *
     * @return the saved memory
     */
    @Transactional
    public Memory save(String content, String type, String source, String tags) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("memory content must not be blank");
        }
        String resolvedType = (type == null || type.isBlank()) ? "NOTE" : type.trim();

        ProposedAction action = new ProposedAction(
            "memory.save", RiskLevel.LOW,
            "Persist a " + resolvedType + " memory", "memory-service");
        approvalGate.evaluate(action);

        Memory memory = new Memory(
            UUID.randomUUID(),
            content.trim(),
            resolvedType,
            source,
            tags,
            Instant.now());
        memory.setEmbedding(Vectors.toCsv(embedder.embed(embeddableText(content, tags))));
        Memory saved = repository.save(memory);
        log.info("Memory saved id={} type={}", saved.getId(), saved.getType());
        return saved;
    }

    /** Keyword recall (READ). Blank query returns the most recent memories. */
    @Transactional(readOnly = true)
    public List<Memory> recall(String query, int limit) {
        int resolvedLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        PageRequest page = PageRequest.of(0, resolvedLimit);
        if (query == null || query.isBlank()) {
            return repository.findAllByOrderByCreatedAtDesc(page);
        }
        return repository.searchByKeyword(query.trim(), page);
    }

    /** Recent memories scanned by the RAG layer for in-Java semantic ranking (READ). */
    @Transactional(readOnly = true)
    public List<Memory> candidatesForRag() {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, RAG_CANDIDATE_WINDOW));
    }

    /** Text fed to the embedder: content plus any tags for a little extra signal. */
    private static String embeddableText(String content, String tags) {
        if (tags == null || tags.isBlank()) {
            return content;
        }
        return content + " " + tags;
    }
}
