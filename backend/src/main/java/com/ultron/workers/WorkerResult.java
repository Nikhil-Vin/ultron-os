package com.ultron.workers;

/**
 * Output of a {@link Worker}.
 *
 * @param success whether the operation completed successfully
 * @param summary short human-readable summary (e.g. the rendered brief)
 * @param detail  optional structured detail or error message
 */
public record WorkerResult(boolean success, String summary, String detail) {

    public static WorkerResult ok(String summary) {
        return new WorkerResult(true, summary, null);
    }

    public static WorkerResult ok(String summary, String detail) {
        return new WorkerResult(true, summary, detail);
    }

    public static WorkerResult fail(String reason) {
        return new WorkerResult(false, reason, null);
    }
}
