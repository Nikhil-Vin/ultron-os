package com.ultron.intelligence.trading;

import org.springframework.stereotype.Component;

/**
 * Risk calculator (Section 7). Position sizing from fixed-fractional risk, the Kelly criterion for
 * optimal fraction, and reward-to-risk. Pure, deterministic, unit-testable — no market access.
 */
@Component
public class RiskCalculator {

    /** Kelly cap — never risk more than this fraction even if Kelly says so (safety). */
    private static final double KELLY_CAP = 0.25;

    /**
     * Fixed-fractional position size: how many units to buy so that hitting the stop loses exactly
     * {@code riskFraction} of the account.
     *
     * @return integer quantity (floored, never negative)
     */
    public int positionSize(double accountValue, double riskFraction, double entry, double stop) {
        double perUnitRisk = Math.abs(entry - stop);
        if (perUnitRisk <= 0 || accountValue <= 0 || riskFraction <= 0) {
            return 0;
        }
        double riskBudget = accountValue * riskFraction;
        return (int) Math.floor(riskBudget / perUnitRisk);
    }

    /**
     * Kelly fraction f* = W - (1-W)/R, where W = win rate, R = avg win / avg loss.
     * Clamped to [0, KELLY_CAP] — negative edge → 0 (don't trade).
     */
    public double kellyFraction(double winRate, double winLossRatio) {
        if (winLossRatio <= 0) {
            return 0.0;
        }
        double f = winRate - (1.0 - winRate) / winLossRatio;
        return Math.max(0.0, Math.min(KELLY_CAP, f));
    }

    /** Reward-to-risk ratio |target-entry| / |entry-stop|. Returns 0 if stop == entry. */
    public double riskReward(double entry, double stop, double target) {
        double risk = Math.abs(entry - stop);
        if (risk <= 0) {
            return 0.0;
        }
        return Math.abs(target - entry) / risk;
    }

    /** A full sizing recommendation. */
    public Sizing recommend(double accountValue, double riskFraction, double entry, double stop, double target) {
        int qty = positionSize(accountValue, riskFraction, entry, stop);
        double rr = riskReward(entry, stop, target);
        double maxLoss = qty * Math.abs(entry - stop);
        double maxGain = qty * Math.abs(target - entry);
        return new Sizing(qty, rr, maxLoss, maxGain);
    }

    /**
     * @param quantity recommended units
     * @param riskReward reward-to-risk ratio
     * @param maxLoss   currency at risk if stopped out
     * @param maxGain   currency gained if target hit
     */
    public record Sizing(int quantity, double riskReward, double maxLoss, double maxGain) {
    }
}
