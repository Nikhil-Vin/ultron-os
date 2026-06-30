package com.ultron.skills;

import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.intelligence.bridge.PythonBridgeClient;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.intelligence.embedding.Vectors;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Skill Intake (L3/L4). Teaching Ultron a new skill is a {@code LOW} risk action: it passes the
 * {@link ApprovalGate} (auto + audited) and is embedded so the RAG layer can ground reasoning on
 * it. Lookup is {@code READ}.
 */
@Service
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);
    private static final int DEFAULT_LIMIT = 25;
    private static final int RAG_CANDIDATE_WINDOW = 500;

    private final SkillRepository repository;
    private final ApprovalGate approvalGate;
    private final EmbedderSelector embedder;
    private final PythonBridgeClient bridgeClient;

    public SkillService(SkillRepository repository, ApprovalGate approvalGate,
                        EmbedderSelector embedder, PythonBridgeClient bridgeClient) {
        this.repository = repository;
        this.approvalGate = approvalGate;
        this.embedder = embedder;
        this.bridgeClient = bridgeClient;
    }

    /**
     * Ingest a new skill (LOW risk → gated + audited, then embedded).
     *
     * @return the saved skill
     */
    @Transactional
    public Skill intake(String name, String description, String content, String tags, String source) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("skill name must not be blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("skill content must not be blank");
        }

        ProposedAction action = new ProposedAction(
            "skill.intake", RiskLevel.LOW,
            "Learn skill '" + name.trim() + "'", "skill-service");
        approvalGate.evaluate(action);

        Skill skill = new Skill(
            UUID.randomUUID(),
            name.trim(),
            description == null ? null : description.trim(),
            content.trim(),
            tags,
            source,
            Instant.now());
        skill.setEmbedding(Vectors.toCsv(embedder.embed(embeddableText(name, description, content, tags))));
        Skill saved = repository.save(skill);
        log.info("Skill learned id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    /** Keyword search over skills (READ). Blank query returns the most recent skills. */
    @Transactional(readOnly = true)
    public List<Skill> search(String query, int limit) {
        int resolvedLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        PageRequest page = PageRequest.of(0, resolvedLimit);
        if (query == null || query.isBlank()) {
            return repository.findAllByOrderByCreatedAtDesc(page);
        }
        return repository.searchByKeyword(query.trim(), page);
    }

    /** Recent active skills scanned by the RAG layer for in-Java semantic ranking (READ). */
    @Transactional(readOnly = true)
    public List<Skill> candidatesForRag() {
        return repository.findActiveSkills(PageRequest.of(0, RAG_CANDIDATE_WINDOW));
    }

    /**
     * Ingest a skill from a rich source (PDF/URL/YouTube) via the ai-layer bridge.
     * Falls back to text-based intake if the bridge is unavailable.
     */
    @Transactional
    public Skill intakeFromSource(String name, String description, String url, String contentType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("skill name must not be blank");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("skill source URL must not be blank");
        }

        ProposedAction action = new ProposedAction(
            "skill.intake-source", RiskLevel.LOW,
            "Learn skill '" + name.trim() + "' from source: " + url, "skill-service");
        approvalGate.evaluate(action);

        // Delegate extraction to ai-layer
        Map<String, Object> bridgeResult = bridgeClient.intakeSkill(name, null, url, contentType);
        String extractedContent;
        int chunkCount = 0;
        if (!bridgeResult.isEmpty()) {
            extractedContent = "Ingested via ai-layer: " + bridgeResult.getOrDefault("chunk_count", 0) + " chunks from " + url;
            Object cc = bridgeResult.get("chunk_count");
            if (cc instanceof Number n) chunkCount = n.intValue();
        } else {
            extractedContent = "Source: " + url + " (ai-layer unavailable, stored as reference)";
        }

        Skill skill = new Skill(
            UUID.randomUUID(),
            name.trim(),
            description == null ? null : description.trim(),
            extractedContent,
            contentType,
            url,
            Instant.now());
        skill.setEmbedding(Vectors.toCsv(embedder.embed(embeddableText(name, description, extractedContent, contentType))));
        Skill saved = repository.save(skill);
        log.info("Skill learned from source id={} name={} chunks={}", saved.getId(), saved.getName(), chunkCount);
        return saved;
    }

    /** Delete a skill by ID (LOW risk — gated + audited). */
    @Transactional
    public boolean delete(UUID id) {
        if (id == null) return false;
        var existing = repository.findById(id);
        if (existing.isEmpty()) return false;

        ProposedAction action = new ProposedAction(
            "skill.delete", RiskLevel.LOW,
            "Delete skill '" + existing.get().getName() + "'", "skill-service");
        approvalGate.evaluate(action);

        repository.deleteById(id);
        log.info("Skill deleted id={}", id);
        return true;
    }

    /** Pause a skill (excluded from RAG retrieval). */
    @Transactional
    public Skill pause(UUID id) {
        Skill skill = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + id));
        skill.setStatus("paused");
        log.info("Skill paused id={} name={}", id, skill.getName());
        return repository.save(skill);
    }

    /** Resume a paused skill. */
    @Transactional
    public Skill resume(UUID id) {
        Skill skill = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + id));
        skill.setStatus("active");
        log.info("Skill resumed id={} name={}", id, skill.getName());
        return repository.save(skill);
    }

    /**
     * Test retrieval against a specific skill: given a query, return how well
     * this skill's embedding matches (score + content preview).
     */
    @Transactional(readOnly = true)
    public TestResult testRetrieval(UUID id, String query) {
        Skill skill = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + id));
        float[] queryVec = embedder.embed(query);
        float[] skillVec = Vectors.fromCsv(skill.getEmbedding());
        float score = (float) Vectors.cosine(queryVec, skillVec);
        String preview = skill.getContent().length() > 200
            ? skill.getContent().substring(0, 200) + "…"
            : skill.getContent();
        return new TestResult(skill.getId(), skill.getName(), score, preview);
    }

    public record TestResult(UUID id, String name, float score, String contentPreview) {
    }

    private static String embeddableText(String name, String description, String content, String tags) {
        StringBuilder sb = new StringBuilder(name);
        if (description != null && !description.isBlank()) {
            sb.append(' ').append(description);
        }
        sb.append(' ').append(content);
        if (tags != null && !tags.isBlank()) {
            sb.append(' ').append(tags);
        }
        return sb.toString();
    }
}
