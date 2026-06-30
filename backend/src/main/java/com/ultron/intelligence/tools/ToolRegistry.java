package com.ultron.intelligence.tools;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry of all {@link Tool} beans (Section 5). Spring injects every tool on the classpath; the
 * brain/agent looks them up by name to invoke. {@link #manifest()} produces the function-calling
 * spec the LLM is shown.
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, Tool> tools;

    public ToolRegistry(List<Tool> toolBeans) {
        this.tools = toolBeans.stream().collect(Collectors.toUnmodifiableMap(Tool::name, Function.identity()));
        log.info("ToolRegistry initialised with tools: {}", tools.keySet());
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }

    public Object invoke(String name, Map<String, Object> args) {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("No tool registered: " + name);
        }
        log.info("Invoking tool={} args={}", name, args == null ? Map.of() : args.keySet());
        return tool.execute(args == null ? Map.of() : args);
    }

    /** Function-calling manifest (name → description) shown to the LLM. */
    public List<Map<String, String>> manifest() {
        return tools.values().stream()
            .map(t -> Map.of("name", t.name(), "description", t.description()))
            .toList();
    }
}
