package com.ultron.workers;

/**
 * A unit of Ultron's staff (L2 — The Staff). Each worker handles a focused responsibility
 * (Sentinel briefs, Planner prioritises, Archivist captures, Scholar researches, Trader watches).
 */
public interface Worker {

    /** Unique registry name, e.g. {@code sentinel}. */
    String name();

    /** Execute a request and return a result. */
    WorkerResult handle(WorkerRequest request);
}
