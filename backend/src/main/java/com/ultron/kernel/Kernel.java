package com.ultron.kernel;

import com.ultron.workers.WorkerRegistry;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Kernel (L1 — Nervous System). Phase 0 responsibility: route an incoming request to
 * the right {@link com.ultron.workers.Worker} and time the dispatch (observability).
 *
 * <p>The full perceive → reason → plan → approve → act → remember agent loop lands in Phase 1
 * ({@code AgentLoop.java}); the kernel is the routing seam it will plug into.
 */
@Component
public class Kernel {

    private static final Logger log = LoggerFactory.getLogger(Kernel.class);

    private final WorkerRegistry workers;

    public Kernel(WorkerRegistry workers) {
        this.workers = workers;
    }

    /**
     * Dispatch a request to a named worker, logging timing and outcome.
     *
     * @param workerName the registered worker name (e.g. {@code sentinel})
     * @param request    the request payload
     * @return the worker's result
     */
    public WorkerResult dispatch(String workerName, WorkerRequest request) {
        long start = System.nanoTime();
        log.info("Kernel dispatch -> worker={} kind={}", workerName, request.kind());
        WorkerResult result = workers.get(workerName).handle(request);
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("Kernel done <- worker={} ok={} durationMs={}", workerName, result.success(), ms);
        return result;
    }
}
