package com.ultron.intelligence.tools;

import java.util.Map;

/**
 * A callable tool the brain can invoke to DO things (Section 5 — Tool Calling). This is the
 * dependency-free analogue of Spring AI's {@code @Tool}: each tool declares a name + description
 * (for the LLM's function-calling spec) and executes against real services. Mutating tools route
 * through the ApprovalGate inside the service they call, so governance is preserved.
 */
public interface Tool {

    /** Unique tool name, e.g. {@code ask}, {@code capture_memory}, {@code trading_signal}. */
    String name();

    /** One-line description for the LLM function-calling manifest. */
    String description();

    /** Execute with named arguments; returns a JSON-serializable result. */
    Object execute(Map<String, Object> args);
}
