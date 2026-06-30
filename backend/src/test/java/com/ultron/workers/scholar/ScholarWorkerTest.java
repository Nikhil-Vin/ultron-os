package com.ultron.workers.scholar;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.config.PythonBridgeConfig;
import com.ultron.config.UltronProperties;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.AuditEntryRepository;
import com.ultron.governance.AuditLog;
import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.HeuristicBrain;
import com.ultron.intelligence.OllamaBrain;
import com.ultron.intelligence.bridge.PythonBridgeClient;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.intelligence.embedding.HeuristicEmbedder;
import com.ultron.intelligence.embedding.OllamaEmbedder;
import com.ultron.intelligence.rag.RagService;
import com.ultron.memory.MemoryRepository;
import com.ultron.memory.MemoryService;
import com.ultron.skills.SkillRepository;
import com.ultron.skills.SkillService;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Scholar research via RAG on in-memory H2. */
@DataJpaTest
class ScholarWorkerTest {

    @Autowired
    private MemoryRepository memoryRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private MemoryService memoryService;
    private ScholarWorker scholar;

    @BeforeEach
    void setUp() {
        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaEnabled(false);
        ApprovalGate gate = new ApprovalGate(props, new AuditLog(auditEntryRepository));
        EmbedderSelector embedder = new EmbedderSelector(new OllamaEmbedder(props), new HeuristicEmbedder());
        BrainSelector brain = new BrainSelector(new OllamaBrain(props), new HeuristicBrain());
        memoryService = new MemoryService(memoryRepository, gate, embedder);
        SkillService skillService = new SkillService(skillRepository, gate, embedder,
            new PythonBridgeClient(new PythonBridgeConfig()));
        RagService rag = new RagService(memoryService, skillService, embedder, brain);
        scholar = new ScholarWorker(rag);
    }

    @Test
    void answersFromMemory() {
        memoryService.save("My AWS region is eu-west-1 for all production stacks", "NOTE", "test", "aws");

        WorkerResult result = scholar.handle(
            new WorkerRequest("research", Map.of("question", "which aws region for production")));

        assertThat(result.success()).isTrue();
        assertThat(result.summary()).contains("[heuristic]");
        assertThat(result.detail()).startsWith("sources=[");
    }

    @Test
    void requiresAQuestion() {
        WorkerResult result = scholar.handle(new WorkerRequest("research", Map.of()));
        assertThat(result.success()).isFalse();
    }
}
