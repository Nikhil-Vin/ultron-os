package com.ultron.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.config.UltronProperties;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.AuditEntryRepository;
import com.ultron.governance.AuditLog;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.intelligence.embedding.HeuristicEmbedder;
import com.ultron.intelligence.embedding.OllamaEmbedder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Integration test on in-memory H2 (no Docker). Verifies that saving routes through the
 * {@link ApprovalGate} (writing an audit entry) and that keyword recall works.
 */
@DataJpaTest
class MemoryServiceTest {

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private MemoryService memoryService;

    @BeforeEach
    void setUp() {
        UltronProperties properties = new UltronProperties(); // auto-approve = false
        properties.getBrain().setOllamaEnabled(false);        // offline embedder, no network
        AuditLog auditLog = new AuditLog(auditEntryRepository);
        ApprovalGate gate = new ApprovalGate(properties, auditLog);
        EmbedderSelector embedder = new EmbedderSelector(new OllamaEmbedder(properties), new HeuristicEmbedder());
        memoryService = new MemoryService(memoryRepository, gate, embedder);
    }

    @Test
    void savesAndRecallsByKeyword() {
        memoryService.save("Reviewed the pgvector index plan for skill chunks", "NOTE", "test", "infra,memory");
        memoryService.save("Bought groceries and paid the electricity bill", "NOTE", "test", "personal");

        List<Memory> hits = memoryService.recall("pgvector", 10);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getContent()).contains("pgvector index plan");
    }

    @Test
    void blankQueryReturnsMostRecentFirst() {
        memoryService.save("first memory", "NOTE", "test", null);
        memoryService.save("second memory", "NOTE", "test", null);

        List<Memory> recent = memoryService.recall("  ", 10);

        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).getContent()).isEqualTo("second memory");
    }

    @Test
    void savingWritesAnAuditEntry() {
        long before = auditEntryRepository.count();

        memoryService.save("an auditable memory", "NOTE", "test", null);

        assertThat(auditEntryRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void blankContentIsRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> memoryService.save("   ", "NOTE", "test", null));
    }
}
