package com.ultron.api;

import com.ultron.memory.Memory;
import com.ultron.memory.MemoryService;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Memory API (L0).
 * <ul>
 *   <li>{@code POST /api/memory} — save a memory (LOW risk → gated + audited).</li>
 *   <li>{@code GET  /api/memory?q=&limit=} — keyword recall (READ).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping
    public MemoryDto save(@RequestBody SaveMemoryRequest request) {
        Memory saved = memoryService.save(
            request.content(),
            request.type(),
            request.source(),
            request.tags());
        return MemoryDto.from(saved);
    }

    @GetMapping
    public List<MemoryDto> recall(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "limit", required = false, defaultValue = "0") int limit) {
        return memoryService.recall(query, limit).stream()
            .map(MemoryDto::from)
            .toList();
    }

    public record SaveMemoryRequest(
        @NotBlank String content,
        String type,
        String source,
        String tags) {
    }

    public record MemoryDto(
        UUID id,
        String content,
        String type,
        String source,
        String tags,
        Instant createdAt) {

        static MemoryDto from(Memory m) {
            return new MemoryDto(m.getId(), m.getContent(), m.getType(),
                m.getSource(), m.getTags(), m.getCreatedAt());
        }
    }
}
