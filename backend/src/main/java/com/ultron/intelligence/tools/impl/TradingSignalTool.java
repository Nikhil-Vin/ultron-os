package com.ultron.intelligence.tools.impl;

import com.ultron.intelligence.tools.Tool;
import com.ultron.intelligence.trading.TradingBrain;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Tool: generate a trading signal + rule-grounded read for an instrument (READ). */
@Component
public class TradingSignalTool implements Tool {

    private final TradingBrain tradingBrain;

    public TradingSignalTool(TradingBrain tradingBrain) {
        this.tradingBrain = tradingBrain;
    }

    @Override
    public String name() {
        return "trading_signal";
    }

    @Override
    public String description() {
        return "Generate a BUY/SELL/HOLD signal + reasoning for an instrument. Args: instrument (string), indicators (object: rsi, macd, macdSignal, sentiment).";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(Map<String, Object> args) {
        String instrument = String.valueOf(args.getOrDefault("instrument", "NIFTY50"));
        Map<String, Double> indicators = new HashMap<>();
        Object raw = args.get("indicators");
        if (raw instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getValue() instanceof Number n) {
                    indicators.put(String.valueOf(e.getKey()), n.doubleValue());
                }
            }
        }
        TradingBrain.Advice advice = tradingBrain.advise(instrument, indicators);
        return Map.of(
            "signal", advice.signal().getSignalType(),
            "confidence", advice.signal().getConfidence(),
            "reasoning", advice.signal().getReasoning(),
            "narrative", advice.narrative());
    }
}
