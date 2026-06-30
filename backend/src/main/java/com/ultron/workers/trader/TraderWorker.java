package com.ultron.workers.trader;

import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.workers.Worker;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Trader (L2 — The Staff). Phase 1 is fully offline and deterministic: it reports a watchlist
 * snapshot and routes trades through the {@link ApprovalGate} to demonstrate Section 14 risk
 * tiers — {@code watch} is READ, {@code paper-trade} is LOW (auto), and {@code live-trade} is
 * CRITICAL (blocked by default until deliberate, voice-confirmed approval in Phase 2).
 */
@Component
public class TraderWorker implements Worker {

    private static final Logger log = LoggerFactory.getLogger(TraderWorker.class);

    /** Deterministic offline watchlist (no network, no broker keys). */
    private static final List<String> WATCHLIST = List.of(
        "AAPL  +0.8%  steady",
        "MSFT  -0.3%  range-bound",
        "NVDA  +2.1%  momentum",
        "BTC   +1.4%  consolidating");

    private final ApprovalGate approvalGate;

    public TraderWorker(ApprovalGate approvalGate) {
        this.approvalGate = approvalGate;
    }

    @Override
    public String name() {
        return "trader";
    }

    /**
     * Supported kinds:
     * <ul>
     *   <li>{@code watch} — READ snapshot of the watchlist.</li>
     *   <li>{@code paper-trade} — LOW (auto-approved): simulated order. Params {@code symbol}, {@code side}, {@code qty}.</li>
     *   <li>{@code live-trade} — CRITICAL (gated): blocked unless explicitly approved.</li>
     * </ul>
     */
    @Override
    public WorkerResult handle(WorkerRequest request) {
        Map<String, Object> p = request.params();
        return switch (request.kind()) {
            case "watch" -> watch();
            case "paper-trade" -> paperTrade(p);
            case "live-trade" -> liveTrade(p);
            default -> WorkerResult.fail("trader: unsupported kind '" + request.kind() + "'");
        };
    }

    private WorkerResult watch() {
        String body = String.join("\n", WATCHLIST);
        log.info("Trader watch snapshot rendered ({} symbols)", WATCHLIST.size());
        return WorkerResult.ok("Watchlist:\n" + body, "symbols=" + WATCHLIST.size());
    }

    private WorkerResult paperTrade(Map<String, Object> p) {
        String order = describeOrder(p);
        ApprovalGate.GateResult gate = approvalGate.evaluate(new ProposedAction(
            "trader.paper-trade", RiskLevel.LOW, "Simulated order: " + order, "trader"));
        // LOW always auto-proceeds.
        return WorkerResult.ok("Paper trade executed (simulated): " + order,
            "decision=" + gate.decision() + " audit=" + gate.auditId());
    }

    private WorkerResult liveTrade(Map<String, Object> p) {
        String order = describeOrder(p);
        boolean humanApproved = Boolean.parseBoolean(String.valueOf(p.getOrDefault("approved", "false")));
        ApprovalGate.GateResult gate = approvalGate.evaluate(new ProposedAction(
            "trader.live-trade", RiskLevel.CRITICAL, "LIVE order: " + order, "trader"), humanApproved);
        if (!gate.allowed()) {
            log.warn("Trader live-trade BLOCKED (decision={})", gate.decision());
            return WorkerResult.fail("Live trade BLOCKED — CRITICAL action requires explicit approval. "
                + "decision=" + gate.decision() + " audit=" + gate.auditId());
        }
        return WorkerResult.ok("Live trade approved + executed: " + order,
            "decision=" + gate.decision() + " audit=" + gate.auditId());
    }

    private static String describeOrder(Map<String, Object> p) {
        String side = String.valueOf(p.getOrDefault("side", "BUY")).toUpperCase();
        String symbol = String.valueOf(p.getOrDefault("symbol", "AAPL")).toUpperCase();
        String qty = String.valueOf(p.getOrDefault("qty", "1"));
        return side + " " + qty + " " + symbol;
    }
}
