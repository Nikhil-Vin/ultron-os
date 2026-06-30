package com.ultron.kernel;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ultron.governance.RiskLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test (Mockito) — verifies the nightly scheduled trigger drives the agent loop, and the
 * morning brief drives the kernel. No Spring context, no scheduler timing.
 */
class SchedulerTest {

    @Test
    void nightlyTriggerInvokesAgentLoop() {
        Kernel kernel = mock(Kernel.class);
        AgentLoop agentLoop = mock(AgentLoop.class);
        when(agentLoop.run(anyString())).thenReturn(new AgentLoop.AgentTrace(
            "review", List.of(), "reasoning", "archivist", "capture",
            RiskLevel.LOW, "AUTO", "audit-1", true, "ok"));

        Scheduler scheduler = new Scheduler(kernel, agentLoop);
        scheduler.nightlyAgentLoop();

        verify(agentLoop).run(anyString());
    }

    @Test
    void morningBriefDispatchesSentinel() {
        Kernel kernel = mock(Kernel.class);
        AgentLoop agentLoop = mock(AgentLoop.class);
        when(kernel.dispatch(org.mockito.ArgumentMatchers.eq("sentinel"), org.mockito.ArgumentMatchers.any()))
            .thenReturn(com.ultron.workers.WorkerResult.ok("brief"));

        Scheduler scheduler = new Scheduler(kernel, agentLoop);
        scheduler.morningBrief();

        verify(kernel).dispatch(org.mockito.ArgumentMatchers.eq("sentinel"), org.mockito.ArgumentMatchers.any());
    }
}
