package com.ultron.api;

import com.ultron.skills.Skill;
import com.ultron.skills.SkillService;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Skill Intake API (L0) — full CRUD per Section 6.
 * <ul>
 *   <li>{@code POST   /api/skills}            — teach a new skill (text, LOW risk).</li>
 *   <li>{@code POST   /api/skills/ingest}     — ingest from URL/PDF/YouTube via ai-layer.</li>
 *   <li>{@code GET    /api/skills?q=&limit=}  — keyword search / recent (READ).</li>
 *   <li>{@code GET    /api/skills/{id}}       — detail view.</li>
 *   <li>{@code DELETE /api/skills/{id}}       — remove a skill (LOW).</li>
 *   <li>{@code POST   /api/skills/{id}/pause} — temporarily exclude from retrieval.</li>
 *   <li>{@code POST   /api/skills/{id}/resume}— re-include in retrieval.</li>
 *   <li>{@code POST   /api/skills/{id}/test}  — test retrieval: query → score for this skill.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping
    public SkillDto intake(@RequestBody IntakeRequest request) {
        Skill saved = skillService.intake(
            request.name(),
            request.description(),
            request.content(),
            request.tags(),
            request.source());
        return SkillDto.from(saved);
    }

    /** Ingest from a rich source (PDF URL, web URL, YouTube link) via the ai-layer bridge. */
    @PostMapping("/ingest")
    public SkillDto ingestFromSource(@RequestBody IngestSourceRequest request) {
        Skill saved = skillService.intakeFromSource(
            request.name(),
            request.description(),
            request.url(),
            request.contentType());
        return SkillDto.from(saved);
    }

    @GetMapping
    public List<SkillDto> search(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "limit", required = false, defaultValue = "0") int limit) {
        return skillService.search(query, limit).stream()
            .map(SkillDto::from)
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillDto> getById(@PathVariable UUID id) {
        return skillService.search(null, 500).stream()
            .filter(s -> s.getId().equals(id))
            .findFirst()
            .map(s -> ResponseEntity.ok(SkillDto.from(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        boolean deleted = skillService.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<SkillDto> pause(@PathVariable UUID id) {
        try {
            Skill paused = skillService.pause(id);
            return ResponseEntity.ok(SkillDto.from(paused));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<SkillDto> resume(@PathVariable UUID id) {
        try {
            Skill resumed = skillService.resume(id);
            return ResponseEntity.ok(SkillDto.from(resumed));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Test retrieval: given a query, see how well this skill scores. */
    @PostMapping("/{id}/test")
    public ResponseEntity<TestResultDto> testRetrieval(
        @PathVariable UUID id, @RequestBody TestRequest request) {
        try {
            SkillService.TestResult result = skillService.testRetrieval(id, request.query());
            return ResponseEntity.ok(new TestResultDto(
                result.id(), result.name(), result.score(), result.contentPreview()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- DTOs ---

    public record IntakeRequest(
        @NotBlank String name,
        String description,
        @NotBlank String content,
        String tags,
        String source) {
    }

    public record IngestSourceRequest(
        @NotBlank String name,
        String description,
        @NotBlank String url,
        String contentType) {
    }

    public record TestRequest(@NotBlank String query) {
    }

    public record SkillDto(
        UUID id,
        String name,
        String description,
        String content,
        String tags,
        String source,
        String status,
        Instant createdAt) {

        static SkillDto from(Skill s) {
            return new SkillDto(s.getId(), s.getName(), s.getDescription(),
                s.getContent(), s.getTags(), s.getSource(), s.getStatus(), s.getCreatedAt());
        }
    }

    public record TestResultDto(UUID id, String name, float score, String contentPreview) {
    }
}
