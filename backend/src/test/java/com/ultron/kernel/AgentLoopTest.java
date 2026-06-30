package com.ultron.kernel;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.config.PythonBridgeConfig;
import com.ultron.config.UltronProperties;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.AuditEntryRepository;
import com.ultron.governance.AuditLog;
import com.ultron.governance.InputSanitizer;
import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.HeuristicBrain;
import com.ultron.intelligence.OllamaBrain;
import com.ultron.intelligence.bridge.PythonBridgeClient;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.intelligence.embedding.HeuristicEmbedder;
import com.ultron.intelligence.embedding.OllamaEmbedder;
import com.ultron.intelligence.psychology.IntentClassifier;
import com.ultron.intelligence.rag.RagService;
import com.ultron.memory.MemoryRepository;
import com.ultron.memory.MemoryService;
import com.ultron.skills.SkillRepository;
import com.ultron.skills.SkillService;
import com.ultron.workers.Worker;
import com.ultron.workers.WorkerRegistry;
import com.ultron.workers.archivist.ArchivistWorker;
import com.ultron.workers.planner.PlannerWorker;
import com.ultron.workers.scholar.ScholarWorker;
import com.ultron.workers.trader.TraderWorker;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Full agent loop on in-memory H2: perceive → reason → plan → approve → act → remember.
 * Verifies a READ instruction acts and is remembered, and a CRITICAL instruction is blocked
 * by the approval gate (human-in-the-loop preserved).
 */
@DataJpaTest
class AgentLoopTest {

    @Autowired
    private MemoryRepository memoryRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private MemoryService memoryService;
    private AgentLoop agentLoop;

    @BeforeEach
    void setUp() {
        UltronProperties props = new UltronProperties(); // auto-approve = false
        props.getBrain().setOllamaEnabled(false);
        ApprovalGate gate = new ApprovalGate(props, new AuditLog(auditEntryRepository));
        EmbedderSelector embedder = new EmbedderSelector(new OllamaEmbedder(props), new HeuristicEmbedder());
        BrainSelector brain = new BrainSelector(new OllamaBrain(props), new HeuristicBrain());

        memoryService = new MemoryService(memoryRepository, gate, embedder);
        SkillService skillService = new SkillService(skillRepository, gate, embedder,
            new PythonBridgeClient(new PythonBridgeConfig()));
        RagService rag = new RagService(memoryService, skillService, embedder, brain);

        List<Worker> workers = List.of(
            new ScholarWorker(rag),
            new ArchivistWorker(memoryService),
            new PlannerWorker(brain, rag),
            new TraderWorker(gate));
        WorkerRegistry registry = new WorkerRegistry(workers);
        Kernel kernel = new Kernel(registry);

        agentLoop = new AgentLoop(rag, brain, gate, registry, kernel, memoryService,
            new InputSanitizer(), new IntentClassifier());
    }

    @Test
    void readInstructionActsAndIsRemembered() {
        memoryService.save("My AWS region is eu-west-1", "NOTE", "test", "aws");
        long before = memoryRepository.count();

        AgentLoop.AgentTrace trace = agentLoop.run("which aws region do I use");

        assertThat(trace.worker()).isEqualTo("scholar");
        assertThat(trace.acted()).isTrue();
        assertThat(trace.decision()).isEqualTo("AUTO");
        // The interaction itself is archived (remember stage).
        assertThat(memoryRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void criticalInstructionIsBlockedByGate() {
        AgentLoop.AgentTrace trace = agentLoop.run("live trade buy AAPL now");

        assertThat(trace.worker()).isEqualTo("trader");
        assertThat(trace.decision()).isEqualTo("DENIED");
        assertThat(trace.acted()).isFalse();
        assertThat(trace.result()).contains("blocked");
    }

    @Test
    void criticalInstructionProceedsWithExplicitApproval() {
        AgentLoop.AgentTrace trace = agentLoop.run("live trade buy AAPL now", true);

        assertThat(trace.worker()).isEqualTo("trader");
        assertThat(trace.decision()).isEqualTo("APPROVED");
        assertThat(trace.acted()).isTrue();
    }
}
