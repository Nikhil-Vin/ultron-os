package com.ultron.intelligence.rag;

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
import com.ultron.memory.MemoryRepository;
import com.ultron.memory.MemoryService;
import com.ultron.skills.SkillRepository;
import com.ultron.skills.SkillService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * RAG on in-memory H2 (offline). Verifies semantic retrieval ranks the relevant memory/skill
 * above unrelated ones, and that the grounded answer is produced by the offline heuristic brain.
 */
@DataJpaTest
class RagServiceTest {

    @Autowired
    private MemoryRepository memoryRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private MemoryService memoryService;
    private SkillService skillService;
    private RagService rag;

    @BeforeEach
    void setUp() {
        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaEnabled(false);
        ApprovalGate gate = new ApprovalGate(props, new AuditLog(auditEntryRepository));
        EmbedderSelector embedder = new EmbedderSelector(new OllamaEmbedder(props), new HeuristicEmbedder());
        BrainSelector brain = new BrainSelector(new OllamaBrain(props), new HeuristicBrain());

        memoryService = new MemoryService(memoryRepository, gate, embedder);
        skillService = new SkillService(skillRepository, gate, embedder,
            new PythonBridgeClient(new PythonBridgeConfig()));
        rag = new RagService(memoryService, skillService, embedder, brain);
    }

    @Test
    void retrievesMostRelevantItemFirst() {
        memoryService.save("Reviewed the pgvector index tuning plan for memory chunks", "NOTE", "test", "infra");
        memoryService.save("Bought groceries and paid the electricity bill", "NOTE", "test", "personal");
        skillService.intake("Tune pgvector", null, "Set ivfflat lists and probes for the vector index", "memory", "test");

        List<RetrievedItem> hits = rag.retrieve("how to tune the pgvector index", 5);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).content().toLowerCase()).contains("pgvector");
        // The grocery memory must not be the top hit.
        assertThat(hits.get(0).content().toLowerCase()).doesNotContain("groceries");
    }

    @Test
    void answerIsGroundedAndProducedByOfflineBrain() {
        memoryService.save("The deploy command is: netlify deploy --prod", "NOTE", "test", "devops");

        RagService.RagAnswer answer = rag.answer("what is the deploy command", 5);

        assertThat(answer.answer()).contains("[heuristic]");
        assertThat(answer.context()).isNotEmpty();
    }

    @Test
    void blankQueryReturnsNoContext() {
        memoryService.save("something", "NOTE", "test", null);
        assertThat(rag.retrieve("   ", 5)).isEmpty();
    }
}
