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
 * scheduled dispatch through the {@link Kernel}. The full nightly agent loop arrives in Phase 1.
 */
@Component
public class Scheduler {

    private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

    private final Kernel kernel;

    public Scheduler(Kernel kernel) {
        this.kernel = kernel;
    }

    /** MORNING BRIEF — 07:00 local every day (Section 9.8). */
    @Scheduled(cron = "${ultron.schedule.morning-brief:0 0 7 * * *}")
    public void morningBrief() {
        log.info("Scheduler trigger: morning brief");
        WorkerResult result = kernel.dispatch("sentinel",
            new WorkerRequest("brief", Map.of("trigger", "schedule")));
        log.info("Morning brief generated ok={}", result.success());
    }
}
