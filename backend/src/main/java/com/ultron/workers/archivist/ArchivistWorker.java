package com.ultron.workers.archivist;

import com.ultron.memory.Memory;
import com.ultron.memory.MemoryService;
import com.ultron.workers.Worker;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Archivist (L2 — The Staff). Captures information into the growing memory (L4). This is a
 * {@code LOW}-risk write that flows through the {@link MemoryService} (gated + audited + embedded).
 */
@Component
public class ArchivistWorker implements Worker {

    private static final Logger log = LoggerFactory.getLogger(ArchivistWorker.class);

    private final MemoryService memoryService;

    public ArchivistWorker(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "archivist";
    }

    /**
     * Supported kinds:
     * <ul>
     *   <li>{@code capture} — params: {@code content} (required), {@code type}, {@code tags}, {@code source}.</li>
     * </ul>
     */
    @Override
    public WorkerResult handle(WorkerRequest request) {
        Map<String, Object> p = request.params();
        String content = str(p.get("content"));
        if (content == null || content.isBlank()) {
            return WorkerResult.fail("archivist: 'content' is required to capture a memory");
        }
        Memory saved = memoryService.save(
            content,
            str(p.get("type")),
            str(p.getOrDefault("source", "archivist")),
            str(p.get("tags")));
        log.info("Archivist captured memory id={}", saved.getId());
        return WorkerResult.ok(
            "Captured memory: " + truncate(saved.getContent(), 80),
            "memory-id=" + saved.getId());
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
