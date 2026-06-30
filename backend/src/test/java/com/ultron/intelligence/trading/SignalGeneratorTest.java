package com.ultron.intelligence.trading;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Signal generation on H2 — deterministic BUY/SELL/HOLD from indicator snapshots. */
@DataJpaTest
@Import(SignalGenerator.class)
class SignalGeneratorTest {

    @Autowired
    private SignalGenerator generator;

    @Test
    void oversoldBullishMacdYieldsBuy() {
        TradingSignal s = generator.generate("NIFTY50",
            Map.of("rsi", 25.0, "macd", 1.2, "macdSignal", 0.8, "sentiment", 0.3));
        assertThat(s.getSignalType()).isEqualTo("BUY");
        assertThat(s.getConfidence().doubleValue()).isGreaterThan(0.0);
        assertThat(s.getId()).isNotNull();
    }

    @Test
    void overboughtBearishYieldsSell() {
        TradingSignal s = generator.generate("RELIANCE",
            Map.of("rsi", 78.0, "macd", 0.5, "macdSignal", 1.0, "sentiment", -0.3));
        assertThat(s.getSignalType()).isEqualTo("SELL");
    }

    @Test
    void neutralYieldsHold() {
        TradingSignal s = generator.generate("BTCUSD",
            Map.of("rsi", 50.0, "macd", 0.1, "macdSignal", 0.1, "sentiment", 0.0));
        assertThat(s.getSignalType()).isEqualTo("HOLD");
    }
}
