package com.ultron.kernel;

import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Time-based triggers (L1). Phase 0 ships the morning-brief cron as a heartbeat that proves
 * scheduled dispatch through the {@link Kernel}. Phase 1 adds the nightly agent loop.
 */
@Component
public class Scheduler {

    private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

    private final Kernel kernel;
    private final AgentLoop agentLoop;

    public Scheduler(Kernel kernel, AgentLoop agentLoop) {
        this.kernel = kernel;
        this.agentLoop = agentLoop;
    }

    /** MORNING BRIEF — 07:00 local every day (Section 9.8). */
    @Scheduled(cron = "${ultron.schedule.morning-brief:0 0 7 * * *}")
    public void morningBrief() {
        log.info("Scheduler trigger: morning brief");
        WorkerResult result = kernel.dispatch("sentinel",
            new WorkerRequest("brief", Map.of("trigger", "schedule")));
        log.info("Morning brief generated ok={}", result.success());
    }

    /**
     * NIGHTLY AGENT LOOP — 23:30 local every day (Section 13, Phase 1).
     * Runs an archival pass: reviews the day's memories and captures a summary.
     */
    @Scheduled(cron = "${ultron.schedule.nightly-agent:0 30 23 * * *}")
    public void nightlyAgentLoop() {
        log.info("Scheduler trigger: nightly agent loop");
        try {
            AgentLoop.AgentTrace trace = agentLoop.run(
                "Review today's memories and interactions. Archive a brief summary of what was accomplished and any open items.");
            log.info("Nightly agent loop complete: worker={} acted={}", trace.worker(), trace.acted());
        } catch (Exception ex) {
            log.warn("Nightly agent loop failed: {}", ex.getMessage());
        }
    }
}
