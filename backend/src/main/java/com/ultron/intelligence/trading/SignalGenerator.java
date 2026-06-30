package com.ultron.intelligence.trading;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signal generator (Section 7). Produces a structured BUY/SELL/HOLD with confidence + reasoning
 * from an indicator snapshot (RSI, MACD, etc.). Deterministic and explainable — the Python
 * {@code signal_engine.py} (TA-Lib + FinBERT + CNN) can override via the bridge later. Every signal
 * is logged whether or not it's acted on.
 */
@Service
public class SignalGenerator {

    private static final Logger log = LoggerFactory.getLogger(SignalGenerator.class);

    private final TradingSignalRepository repository;

    public SignalGenerator(TradingSignalRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate + persist a signal.
     *
     * @param indicators e.g. {rsi: 28.0, macd: 1.2, macdSignal: 0.9, sentiment: 0.6}
     */
    @Transactional
    public TradingSignal generate(String instrument, Map<String, Double> indicators) {
        double rsi = indicators.getOrDefault("rsi", 50.0);
        double macd = indicators.getOrDefault("macd", 0.0);
        double macdSignal = indicators.getOrDefault("macdSignal", 0.0);
        double sentiment = indicators.getOrDefault("sentiment", 0.0);

        List<String> reasons = new ArrayList<>();
        double score = 0.0; // >0 → bullish, <0 → bearish

        if (rsi < 30) { score += 0.4; reasons.add("RSI " + rsi + " oversold"); }
        else if (rsi > 70) { score -= 0.4; reasons.add("RSI " + rsi + " overbought"); }

        if (macd > macdSignal) { score += 0.3; reasons.add("MACD bullish crossover"); }
        else if (macd < macdSignal) { score -= 0.3; reasons.add("MACD bearish crossover"); }

        if (sentiment > 0.2) { score += 0.2; reasons.add("positive news sentiment"); }
        else if (sentiment < -0.2) { score -= 0.2; reasons.add("negative news sentiment"); }

        String type;
        if (score >= 0.3) type = "BUY";
        else if (score <= -0.3) type = "SELL";
        else type = "HOLD";

        double confidence = Math.min(1.0, Math.abs(score));
        TradingSignal signal = new TradingSignal(
            UUID.randomUUID(), instrument, type,
            BigDecimal.valueOf(confidence).setScale(4, java.math.RoundingMode.HALF_UP),
            String.join("; ", reasons),
            indicators.toString(),
            BigDecimal.valueOf(sentiment).setScale(4, java.math.RoundingMode.HALF_UP));
        TradingSignal saved = repository.save(signal);
        log.info("Signal {} {} conf={} ({})", type, instrument, confidence, reasons);
        return saved;
    }
}
