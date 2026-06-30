package com.ultron.api;

import com.ultron.intelligence.tools.ToolRegistry;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tool API (L0 — Section 5). {@code GET /api/tools} returns the function-calling manifest;
 * {@code POST /api/tools/{name}} invokes a tool. Mutating tools enforce governance inside the
 * services they call.
 */
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolRegistry registry;

    public ToolController(ToolRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<Map<String, String>> manifest() {
        return registry.manifest();
    }

    @PostMapping("/{name}")
    public ResponseEntity<Object> invoke(@PathVariable String name, @RequestBody(required = false) InvokeRequest req) {
        if (!registry.has(name)) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> args = req == null || req.args() == null ? Map.of() : req.args();
        return ResponseEntity.ok(registry.invoke(name, args));
    }

    public record InvokeRequest(Map<String, Object> args) {
    }

    // Reserved for future typed invocation.
    public record NamedInvoke(@NotBlank String name, Map<String, Object> args) {
    }
}
