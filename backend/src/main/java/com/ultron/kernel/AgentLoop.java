package com.ultron.kernel;

import com.ultron.governance.ApprovalGate;
import com.ultron.governance.InputSanitizer;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.psychology.IntentClassifier;
import com.ultron.intelligence.psychology.IntentClassifier.Intent;
import com.ultron.intelligence.rag.RagService;
import com.ultron.intelligence.rag.RetrievedItem;
import com.ultron.memory.MemoryService;
import com.ultron.workers.WorkerRegistry;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The agent loop (L1 — Nervous System): perceive → reason → plan → approve → act → remember.
 *
 * <p>This is the full cognitive cycle the Phase 0 kernel left as a seam. Given an instruction it:
 * <ol>
 *   <li><b>perceive</b> — retrieve relevant memories + skills (RAG, L3);</li>
 *   <li><b>reason</b> — have the active brain interpret the instruction in that context;</li>
 *   <li><b>plan</b> — decide whether a worker should act and at what {@link RiskLevel};</li>
 *   <li><b>approve</b> — run the proposed action through the {@link ApprovalGate} (L6);</li>
 *   <li><b>act</b> — dispatch to the chosen worker only if the gate allows;</li>
 *   <li><b>remember</b> — archive the interaction back into memory (L4).</li>
 * </ol>
 *
 * <p>Human-in-the-loop is preserved: HIGH/CRITICAL plans are blocked by default and the loop
 * returns the audit id of the blocked attempt instead of acting.
 */
@Component
public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);

    private final RagService rag;
    private final BrainSelector brain;
    private final ApprovalGate approvalGate;
    private final WorkerRegistry workers;
    private final Kernel kernel;
    private final MemoryService memoryService;
    private final InputSanitizer sanitizer;
    private final IntentClassifier intentClassifier;

    public AgentLoop(RagService rag, BrainSelector brain, ApprovalGate approvalGate,
                     WorkerRegistry workers, Kernel kernel, MemoryService memoryService,
                     InputSanitizer sanitizer, IntentClassifier intentClassifier) {
        this.rag = rag;
        this.brain = brain;
        this.approvalGate = approvalGate;
        this.workers = workers;
        this.kernel = kernel;
        this.memoryService = memoryService;
        this.sanitizer = sanitizer;
        this.intentClassifier = intentClassifier;
    }

    /**
     * Run the full loop for an instruction with no explicit human approval (the common path).
     *
     * @param instruction what the owner asked for
     * @return a structured trace of every stage
     */
    public AgentTrace run(String instruction) {
        return run(instruction, false);
    }

    /**
     * Run the full loop, optionally carrying an explicit human approval for the proposed action.
     * This is the interactive human-in-the-loop path: a HIGH/CRITICAL action that was blocked on a
     * first pass can be re-run with {@code humanApproved=true} to proceed (still fully audited).
     *
     * @param instruction   what the owner asked for
     * @param humanApproved true if the operator has explicitly approved this action
     * @return a structured trace of every stage
     */
    public AgentTrace run(String instruction, boolean humanApproved) {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("agent instruction must not be blank");
        }
        // 0. SANITISE — clean untrusted input before it reaches the brain or memory (L6).
        InputSanitizer.Result screened = sanitizer.screen(instruction);
        String task = screened.sanitized();
        if (task.isBlank()) {
            throw new IllegalArgumentException("agent instruction must not be blank after sanitisation");
        }
        if (screened.suspicious()) {
            log.warn("AgentLoop received input flagged as possible prompt-injection; treating as data only");
        }

        // 1. PERCEIVE
        List<RetrievedItem> context = rag.retrieve(task, 4);

        // 2. REASON
        StringBuilder reasonPrompt = new StringBuilder("Instruction: ").append(task).append('\n');
        if (!context.isEmpty()) {
            reasonPrompt.append("Known context:\n");
            for (RetrievedItem item : context) {
                reasonPrompt.append(" - ").append(item.content()).append('\n');
            }
        }
        reasonPrompt.append("Briefly state how to handle this.");
        String reasoning = brain.think(reasonPrompt.toString());

        // 3. PLAN — choose a worker + risk from the instruction (deterministic, conservative).
        Plan plan = plan(task);

        // 4. APPROVE — carries the explicit human-approval signal when re-run from the gate modal.
        ProposedAction action = new ProposedAction(
            "agent." + plan.worker() + "." + plan.kind(), plan.risk(),
            "Agent loop: " + task, "agent-loop");
        ApprovalGate.GateResult gate = approvalGate.evaluate(action, humanApproved);

        // 5. ACT (only if allowed and the worker exists)
        WorkerResult actResult;
        boolean acted;
        if (!gate.allowed()) {
            acted = false;
            actResult = WorkerResult.fail("Action blocked by approval gate (risk=" + plan.risk()
                + ", decision=" + gate.decision() + "). Awaiting human approval.");
        } else if (!workers.has(plan.worker())) {
            acted = false;
            actResult = WorkerResult.ok(reasoning, "no-worker-dispatched");
        } else {
            // Carry the gate's approval downstream so a re-gating worker (e.g. Trader live-trade)
            // stays consistent with the loop's decision instead of blocking again.
            Map<String, Object> params = new java.util.HashMap<>(plan.params());
            params.putIfAbsent("approved", String.valueOf(gate.decision() == com.ultron.governance.Decision.APPROVED));
            actResult = kernel.dispatch(plan.worker(), new WorkerRequest(plan.kind(), params));
            acted = actResult.success();
        }

        // 6. REMEMBER
        String memo = "Agent handled: " + task + " → " + (actResult.success() ? "ok" : "blocked/failed");
        memoryService.save(memo, "AGENT", "agent-loop", "agent");

        log.info("AgentLoop complete worker={} kind={} risk={} acted={}",
            plan.worker(), plan.kind(), plan.risk(), acted);

        return new AgentTrace(task, context, reasoning, plan.worker(), plan.kind(),
            plan.risk(), gate.decision().toString(), gate.auditId(), acted, actResult.summary());
    }

    /**
     * Map the instruction to a worker + risk via the {@link IntentClassifier} (L7). Conservative
     * by default — a live-trade intent is CRITICAL (and therefore gated).
     */
    private Plan plan(String task) {
        Intent intent = intentClassifier.classify(task);
        return switch (intent) {
            case CAPTURE -> new Plan("archivist", "capture", RiskLevel.LOW, Map.of("content", task, "tags", "agent"));
            case PLAN -> new Plan("planner", "plan", RiskLevel.READ, Map.of("goal", task));
            case TRADE_WATCH -> new Plan("trader", "watch", RiskLevel.READ, Map.of());
            case TRADE_LIVE -> new Plan("trader", "live-trade", RiskLevel.CRITICAL, Map.of());
            case QUESTION, SMALL_TALK -> new Plan("scholar", "research", RiskLevel.READ, Map.of("question", task));
        };
    }

    /** Internal plan record. */
    private record Plan(String worker, String kind, RiskLevel risk, Map<String, Object> params) {
    }

    /**
     * A full trace of one agent-loop run.
     */
    public record AgentTrace(
        String instruction,
        List<RetrievedItem> perceived,
        String reasoning,
        String worker,
        String kind,
        RiskLevel risk,
        String decision,
        String auditId,
        boolean acted,
        String result) {
    }
}
