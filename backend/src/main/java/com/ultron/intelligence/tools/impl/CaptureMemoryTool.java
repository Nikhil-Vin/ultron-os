package com.ultron.intelligence.tools.impl;

import com.ultron.intelligence.tools.Tool;
import com.ultron.memory.Memory;
import com.ultron.memory.MemoryService;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Tool: capture a memory (LOW — gated + audited inside MemoryService). */
@Component
public class CaptureMemoryTool implements Tool {

    private final MemoryService memoryService;

    public CaptureMemoryTool(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "capture_memory";
    }

    @Override
    public String description() {
        return "Save something to long-term memory. Args: content (string, required), type, tags (optional).";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String content = String.valueOf(args.getOrDefault("content", ""));
        if (content.isBlank()) {
            return Map.of("error", "content is required");
        }
        Memory saved = memoryService.save(content,
            asString(args.get("type")), "tool", asString(args.get("tags")));
        return Map.of("id", saved.getId().toString(), "type", saved.getType());
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
