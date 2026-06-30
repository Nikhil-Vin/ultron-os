package com.ultron.connectors.broker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Paper broker (L5) — the ALWAYS-safe, ALWAYS-wired default (Section 7). Simulates fills against a
 * deterministic synthetic quote so the full trading stack (signals, risk, journal, P&L, psychology)
 * works end-to-end with zero real money and zero external keys.
 */
@Component
@Primary
public class PaperBroker implements BrokerConnector {

    private static final Logger log = LoggerFactory.getLogger(PaperBroker.class);

    @Override
    public String name() {
        return "paper";
    }

    @Override
    public String mode() {
        return "paper";
    }

    @Override
    public boolean isLiveWired() {
        return false; // by definition — this is simulation
    }

    @Override
    public Quote quote(String instrument) {
        // Deterministic synthetic price seeded by the instrument name + a small intraday jitter.
        long seed = Math.abs(instrument.hashCode());
        double base = 100 + (seed % 4000);
        double jitter = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
        BigDecimal price = BigDecimal.valueOf(base + jitter).setScale(2, RoundingMode.HALF_UP);
        return new Quote(instrument, price, "paper-sim");
    }

    @Override
    public OrderResult placeOrder(OrderRequest request) {
        BigDecimal fill = request.limitPrice() != null ? request.limitPrice() : quote(request.instrument()).price();
        String orderId = "PAPER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("PAPER order {} {} x{} @ {} -> {}", request.side(), request.instrument(),
            request.quantity(), fill, orderId);
        return new OrderResult(orderId, "FILLED", "paper", fill,
            "Simulated fill (paper trading — no real money).");
    }
}
