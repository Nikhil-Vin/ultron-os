package com.ultron.intelligence.trading;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Trading psychology monitor (Section 8 / 7). Flags FOMO, revenge trading, overtrading, and rule
 * violations from recent trade history, and tracks a rolling discipline score. Read-only analysis.
 */
@Component
public class TradingPsychMonitor {

    private static final int MAX_TRADES_PER_DAY = 3;
    private static final Duration RECENT_WINDOW = Duration.ofHours(1);

    private final TradeRepository trades;

    public TradingPsychMonitor(TradeRepository trades) {
        this.trades = trades;
    }

    /**
     * Evaluate the psychological context for a proposed trade.
     *
     * @param missedInitialMove true if entering after the move already started (FOMO signal)
     * @param matchesRule       true if the setup matches an ingested/defined rule
     */
    public Assessment assess(boolean missedInitialMove, boolean matchesRule) {
        List<Trade> recent = trades.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50));
        Instant now = Instant.now();

        long todayCount = recent.stream()
            .filter(t -> t.getCreatedAt() != null
                && Duration.between(t.getCreatedAt(), now).toHours() < 24)
            .count();

        long recentLosses = recent.stream()
            .filter(t -> t.getCreatedAt() != null
                && Duration.between(t.getCreatedAt(), now).compareTo(RECENT_WINDOW) < 0)
            .filter(t -> t.getPnl() != null && t.getPnl().signum() < 0)
            .count();

        List<String> flags = new ArrayList<>();
        if (missedInitialMove) {
            flags.add("FOMO");
        }
        if (recentLosses >= 2) {
            flags.add("revenge_trade");
        }
        if (todayCount >= MAX_TRADES_PER_DAY) {
            flags.add("overtrading");
        }
        if (!matchesRule) {
            flags.add("rule_violation");
        }

        double discipline = disciplineScore(recent);
        return new Assessment(flags, discipline, (int) todayCount, (int) recentLosses);
    }

    /** Rolling discipline = fraction of recent trades with no psychology flags. */
    public double disciplineScore(List<Trade> recent) {
        if (recent.isEmpty()) {
            return 1.0;
        }
        long clean = recent.stream()
            .filter(t -> t.getPsychologyFlags() == null || t.getPsychologyFlags().isBlank())
            .count();
        return (double) clean / recent.size();
    }

    /**
     * @param flags         detected psychology flags
     * @param disciplineScore rolling 0..1 compliance score
     * @param tradesToday   count in the last 24h
     * @param recentLosses  losses in the last hour
     */
    public record Assessment(List<String> flags, double disciplineScore, int tradesToday, int recentLosses) {
        public boolean clean() {
            return flags.isEmpty();
        }
    }
}
