package com.ultron.memory;

import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
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
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
    private static final int DEFAULT_LIMIT = 25;

    private final MemoryRepository repository;
    private final ApprovalGate approvalGate;

    public MemoryService(MemoryRepository repository, ApprovalGate approvalGate) {
        this.repository = repository;
        this.approvalGate = approvalGate;
    }

    /**
     * Persist a new memory. Passes through the approval gate (LOW risk → auto + audited).
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
}
