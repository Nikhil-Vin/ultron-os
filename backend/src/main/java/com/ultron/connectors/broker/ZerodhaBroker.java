package com.ultron.connectors.broker;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Zerodha Kite broker (L5 — India). Read-only market data is safe to wire; the LIVE order path is a
 * deliberately-disabled extension point per Section 7 / the moat.
 *
 * <p>This bean is NOT the default trading broker — {@link PaperBroker} is. It exists so live
 * execution can be wired by the owner, deliberately, later.
 */
@Component
public class ZerodhaBroker implements BrokerConnector {

    private static final Logger log = LoggerFactory.getLogger(ZerodhaBroker.class);

    @Override
    public String name() {
        return "zerodha";
    }

    @Override
    public String mode() {
        return "live-extension-point";
    }

    @Override
    public boolean isLiveWired() {
        // Stays false until the owner implements + enables placeOrder() below. Never auto-true.
        return false;
    }

    @Override
    public Quote quote(String instrument) {
        // TODO(owner): wire Kite Connect quote API (read-only, safe). Until then, no live data.
        // Returning a clearly-labelled placeholder so the UI shows "not connected" rather than fake prices.
        return new Quote(instrument, BigDecimal.ZERO, "zerodha-not-connected");
    }

    @Override
    public OrderResult placeOrder(OrderRequest request) {
        // ===================================================================================
        // LIVE ORDER PLACEMENT — DELIBERATELY NOT WIRED.
        // -----------------------------------------------------------------------------------
        // Wiring this to fire a REAL order on Zerodha Kite means REAL MONEY moves. Per Section 7
        // and the Section 3 moat, Ultron ships this disabled. To enable, the OWNER must:
        //   1. Add the kiteconnect dependency + Kite Connect API key/secret in env (never in code).
        //   2. Implement the real KiteConnect.placeOrder(...) call here.
        //   3. Flip isLiveWired() to read a config flag you set deliberately (e.g. EXECUTION_MODE=live).
        // It is intentionally never auto-completed. Paper trading remains the default everywhere.
        // ===================================================================================
        throw new LiveExecutionNotWiredException(
            "Zerodha live order placement is not wired. This is intentional (Section 7 moat): "
                + "real-money execution must be deliberately enabled by the owner. Use the paper broker.");
    }
}
