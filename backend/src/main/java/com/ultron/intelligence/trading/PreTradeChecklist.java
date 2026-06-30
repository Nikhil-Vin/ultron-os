package com.ultron.intelligence.trading;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pre-trade checklist (Section 7). Validates a proposed trade against rule-based criteria before it
 * may proceed: session timing, reward-to-risk minimum, daily-loss limit, and the psychology
 * assessment. Returns a structured pass/fail with reasons — advisory for paper, enforced for live.
 */
@Component
public class PreTradeChecklist {

    private static final double MIN_RR = 2.0;
    /** Avoid the first 15 minutes after a 09:15 open (Indian market convention; configurable later). */
    private static final LocalTime SESSION_OPEN = LocalTime.of(9, 15);
    private static final LocalTime SESSION_SETTLED = LocalTime.of(9, 30);
    private static final LocalTime SESSION_CLOSE = LocalTime.of(15, 30);

    private final RiskCalculator riskCalculator;

    public PreTradeChecklist(RiskCalculator riskCalculator) {
        this.riskCalculator = riskCalculator;
    }

    public Result run(double entry, double stop, double target, double dailyPnl, double dailyLossLimit,
                      TradingPsychMonitor.Assessment psych, LocalTime now) {
        List<Check> checks = new ArrayList<>();

        double rr = riskCalculator.riskReward(entry, stop, target);
        checks.add(new Check("reward_to_risk", rr >= MIN_RR,
            String.format("R:R %.2f (min %.1f)", rr, MIN_RR)));

        boolean inSession = !now.isBefore(SESSION_SETTLED) && !now.isAfter(SESSION_CLOSE);
        boolean firstFifteen = !now.isBefore(SESSION_OPEN) && now.isBefore(SESSION_SETTLED);
        checks.add(new Check("session_timing", inSession,
            firstFifteen ? "avoid first 15 minutes after open" : (inSession ? "ok" : "outside session")));

        boolean lossOk = dailyPnl > -Math.abs(dailyLossLimit);
        checks.add(new Check("daily_loss_limit", lossOk,
            String.format("daily P&L %.2f vs limit -%.2f", dailyPnl, Math.abs(dailyLossLimit))));

        checks.add(new Check("psychology", psych.clean(),
            psych.clean() ? "no flags" : "flags: " + String.join(",", psych.flags())));

        boolean passed = checks.stream().allMatch(Check::passed);
        return new Result(passed, rr, checks);
    }

    /** A single check outcome. */
    public record Check(String name, boolean passed, String detail) {
    }

    /** Overall checklist result. */
    public record Result(boolean passed, double riskReward, List<Check> checks) {
    }
}
