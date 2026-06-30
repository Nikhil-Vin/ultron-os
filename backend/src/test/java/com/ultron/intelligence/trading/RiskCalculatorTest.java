package com.ultron.intelligence.trading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class RiskCalculatorTest {

    private final RiskCalculator calc = new RiskCalculator();

    @Test
    void positionSizeRespectsFixedFractionalRisk() {
        // ₹500000 account, risk 2% = ₹10000; stop is 70 away → 142 units.
        int qty = calc.positionSize(500_000, 0.02, 22_450, 22_380);
        assertThat(qty).isEqualTo(142);
    }

    @Test
    void kellyClampsNegativeEdgeToZeroAndCaps() {
        assertThat(calc.kellyFraction(0.3, 1.0)).isEqualTo(0.0);   // negative edge → don't trade
        assertThat(calc.kellyFraction(0.9, 5.0)).isLessThanOrEqualTo(0.25); // capped
    }

    @Test
    void riskRewardComputesRatio() {
        assertThat(calc.riskReward(22_450, 22_380, 22_580)).isCloseTo(1.857, within(0.01));
    }

    @Test
    void recommendBundlesSizingAndRr() {
        RiskCalculator.Sizing s = calc.recommend(500_000, 0.02, 100, 95, 115);
        assertThat(s.quantity()).isGreaterThan(0);
        assertThat(s.riskReward()).isCloseTo(3.0, within(0.001));
        assertThat(s.maxLoss()).isCloseTo(s.quantity() * 5.0, within(0.001));
    }
}
