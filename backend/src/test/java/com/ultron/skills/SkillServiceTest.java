package com.ultron.skills;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ultron.config.PythonBridgeConfig;
import com.ultron.config.UltronProperties;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.AuditEntryRepository;
import com.ultron.governance.AuditLog;
import com.ultron.intelligence.bridge.PythonBridgeClient;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.intelligence.embedding.HeuristicEmbedder;
import com.ultron.intelligence.embedding.OllamaEmbedder;
import com.ultron.intelligence.embedding.Vectors;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Skill Intake on in-memory H2. Verifies intake is gated + audited + embedded and that search works.
 */
@DataJpaTest
class SkillServiceTest {

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private SkillService skillService;

    @BeforeEach
    void setUp() {
        UltronProperties props = new UltronProperties();
        props.getBrain().setOllamaEnabled(false);
        ApprovalGate gate = new ApprovalGate(props, new AuditLog(auditEntryRepository));
        EmbedderSelector embedder = new EmbedderSelector(new OllamaEmbedder(props), new HeuristicEmbedder());
        PythonBridgeConfig bridgeConfig = new PythonBridgeConfig(); // disabled by default
        PythonBridgeClient bridgeClient = new PythonBridgeClient(bridgeConfig);
        skillService = new SkillService(skillRepository, gate, embedder, bridgeClient);
    }

    @Test
    void intakeStoresEmbeddedSkillAndAudits() {
        long auditsBefore = auditEntryRepository.count();

        Skill skill = skillService.intake(
            "Deploy to Netlify",
            "How to ship the frontend",
            "Run npm build then netlify deploy --prod",
            "devops,frontend",
            "test");

        assertThat(skill.getId()).isNotNull();
        assertThat(Vectors.fromCsv(skill.getEmbedding())).isNotEmpty();
        assertThat(auditEntryRepository.count()).isEqualTo(auditsBefore + 1);
    }

    @Test
    void searchFindsSkillByKeyword() {
        skillService.intake("Tune pgvector", null, "Set lists and probes for ivfflat index", "memory", "test");
        skillService.intake("Make coffee", null, "Grind beans and brew", "kitchen", "test");

        List<Skill> hits = skillService.search("pgvector", 10);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getName()).isEqualTo("Tune pgvector");
    }

    @Test
    void blankContentIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> skillService.intake("name", null, "   ", null, "test"));
    }

    @Test
    void deleteRemovesSkill() {
        Skill s = skillService.intake("Temp skill", null, "some content", null, "test");
        assertThat(skillService.delete(s.getId())).isTrue();
        assertThat(skillRepository.findById(s.getId())).isEmpty();
        // Deleting a non-existent id returns false.
        assertThat(skillService.delete(java.util.UUID.randomUUID())).isFalse();
    }

    @Test
    void pausedSkillIsExcludedFromRagButResumable() {
        Skill s = skillService.intake("Pausable", null, "pgvector index tuning content", "memory", "test");

        skillService.pause(s.getId());
        assertThat(skillRepository.findById(s.getId()).get().getStatus()).isEqualTo("paused");
        assertThat(skillService.candidatesForRag()).noneMatch(sk -> sk.getId().equals(s.getId()));

        skillService.resume(s.getId());
        assertThat(skillService.candidatesForRag()).anyMatch(sk -> sk.getId().equals(s.getId()));
    }

    @Test
    void testRetrievalScoresRelevantQueryHigher() {
        Skill s = skillService.intake("Deploy", null, "netlify deploy prod frontend build", "devops", "test");
        SkillService.TestResult relevant = skillService.testRetrieval(s.getId(), "how to deploy frontend to netlify");
        SkillService.TestResult unrelated = skillService.testRetrieval(s.getId(), "cooking pasta recipe");
        assertThat(relevant.score()).isGreaterThan(unrelated.score());
    }
}
