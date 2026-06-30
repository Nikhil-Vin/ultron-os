package com.ultron.api;

import com.ultron.kernel.AgentLoop;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/agent} — run the full agent loop (perceive → reason → plan → approve → act →
 * remember) for an instruction. The response includes the decision + audit id, so a blocked
 * HIGH/CRITICAL action is fully traceable.
 *
 * <p>{@code POST /api/agent/approve} re-runs the same instruction with an explicit human approval,
 * letting a previously-blocked CRITICAL action proceed (still audited) — the interactive
 * human-in-the-loop path behind the UI's gate modal.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentLoop agentLoop;

    public AgentController(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    @PostMapping
    public AgentLoop.AgentTrace run(@RequestBody RunRequest request) {
        return agentLoop.run(request.instruction());
    }

    @PostMapping("/approve")
    public AgentLoop.AgentTrace approve(@RequestBody RunRequest request) {
        return agentLoop.run(request.instruction(), true);
    }

    public record RunRequest(@NotBlank String instruction) {
    }
}
