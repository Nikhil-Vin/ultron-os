package com.ultron.connectors.broker;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Alpaca broker (L5 — US). Alpaca offers free paper trading via its own API; that and read-only
 * data are safe to wire. The LIVE order path is a deliberately-disabled extension point per
 * Section 7 / the moat. {@link PaperBroker} remains Ultron's default simulation broker.
 */
@Component
public class AlpacaBroker implements BrokerConnector {

    private static final Logger log = LoggerFactory.getLogger(AlpacaBroker.class);

    @Override
    public String name() {
        return "alpaca";
    }

    @Override
    public String mode() {
        return "live-extension-point";
    }

    @Override
    public boolean isLiveWired() {
        return false; // never auto-true; owner enables deliberately
    }

    @Override
    public Quote quote(String instrument) {
        // TODO(owner): wire Alpaca market-data API (paper data is free). Placeholder until then.
        return new Quote(instrument, BigDecimal.ZERO, "alpaca-not-connected");
    }

    @Override
    public OrderResult placeOrder(OrderRequest request) {
        // ===================================================================================
        // LIVE ORDER PLACEMENT — DELIBERATELY NOT WIRED (Section 7 moat).
        // To enable real (or even Alpaca-paper) execution, the OWNER must:
        //   1. Add alpaca-trade-api creds in env (never in code).
        //   2. Implement the real Alpaca REST placeOrder(...) here.
        //   3. Gate it behind a config flag you set deliberately.
        // Ultron's internal PaperBroker simulation is the default; this stays off until wired.
        // ===================================================================================
        throw new LiveExecutionNotWiredException(
            "Alpaca live order placement is not wired. This is intentional (Section 7 moat). "
                + "Use the paper broker; wire Alpaca yourself when ready.");
    }
}
