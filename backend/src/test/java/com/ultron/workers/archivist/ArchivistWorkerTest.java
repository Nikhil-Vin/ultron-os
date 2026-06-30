package com.ultron.workers.archivist;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.config.UltronProperties;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.AuditEntryRepository;
import com.ultron.governance.AuditLog;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.intelligence.embedding.HeuristicEmbedder;
import com.ultron.intelligence.embedding.OllamaEmbedder;
import com.ultron.memory.MemoryRepository;
import com.ultron.memory.MemoryService;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Archivist capture on in-memory H2. */
@DataJpaTest
class ArchivistWorkerTest {

    @Autowired
    private MemoryRepository memoryRepository;
    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private ArchivistWorker archivist;

    @BeforeEach
    void setUp() {
        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaEnabled(false);
        ApprovalGate gate = new ApprovalGate(props, new AuditLog(auditEntryRepository));
        EmbedderSelector embedder = new EmbedderSelector(new OllamaEmbedder(props), new HeuristicEmbedder());
        archivist = new ArchivistWorker(new MemoryService(memoryRepository, gate, embedder));
    }

    @Test
    void capturesAMemory() {
        WorkerResult result = archivist.handle(
            new WorkerRequest("capture", Map.of("content", "Remember to renew the domain", "tags", "ops")));

        assertThat(result.success()).isTrue();
        assertThat(result.detail()).startsWith("memory-id=");
        assertThat(memoryRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsMissingContent() {
        WorkerResult result = archivist.handle(new WorkerRequest("capture", Map.of()));
        assertThat(result.success()).isFalse();
    }
}
