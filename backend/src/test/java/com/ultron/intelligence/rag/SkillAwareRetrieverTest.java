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

/** Verifies the skill boost promotes a learned skill when memory + skill match similarly. */
@DataJpaTest
class SkillAwareRetrieverTest {

    @Autowired
    private MemoryRepository memoryRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private MemoryService memoryService;
    private SkillService skillService;
    private SkillAwareRetriever retriever;

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
        RagService rag = new RagService(memoryService, skillService, embedder, brain);
        retriever = new SkillAwareRetriever(rag);
    }

    @Test
    void skillIsBoostedAboveSimilarMemory() {
        // Same vocabulary in both a memory and a skill.
        memoryService.save("deploy frontend netlify production build command", "NOTE", "test", "devops");
        skillService.intake("Deploy frontend", null,
            "deploy frontend netlify production build command", "devops", "test");

        List<RetrievedItem> hits = retriever.retrieve("how to deploy frontend to netlify production", 5);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).kind()).isEqualTo("skill");
    }
}
