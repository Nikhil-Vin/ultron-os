package com.ultron.connectors.broker;

import java.math.BigDecimal;

/**
 * Broker abstraction (L5 — Section 7 / 11.7).
 *
 * <p><b>Moat rule:</b> {@link #placeOrder} for a LIVE broker is a deliberately-gated extension
 * point. Paper trading is fully wired and the default; real order placement ships disabled and the
 * owner wires it themselves. Live brokers throw {@link LiveExecutionNotWiredException} until that
 * deliberate step is taken — it is never auto-completed.
 */
public interface BrokerConnector {

    /** e.g. {@code paper}, {@code zerodha}, {@code alpaca}. */
    String name();

    /** {@code paper} (fully wired) or {@code live-extension-point} (stubbed until wired). */
    String mode();

    /** True only when a real live order path has been deliberately implemented + enabled. */
    boolean isLiveWired();

    /** Latest quote for an instrument (read-only; safe). */
    Quote quote(String instrument);

    /** Place an order. Paper → simulated fill. Live brokers → throws until wired by the owner. */
    OrderResult placeOrder(OrderRequest request);

    /** An order to place. */
    record OrderRequest(String instrument, String side, int quantity, BigDecimal limitPrice, String type) {
        public OrderRequest {
            if (instrument == null || instrument.isBlank()) {
                throw new IllegalArgumentException("instrument required");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            side = side == null ? "BUY" : side.toUpperCase();
            type = type == null ? "MARKET" : type.toUpperCase();
        }
    }

    /** Result of an order. */
    record OrderResult(String orderId, String status, String executionMode,
                       BigDecimal filledPrice, String message) {
    }

    /** A market quote. */
    record Quote(String instrument, BigDecimal price, String source) {
    }

    /** Thrown when a live order is attempted before the owner has wired the real broker path. */
    class LiveExecutionNotWiredException extends RuntimeException {
        public LiveExecutionNotWiredException(String message) {
            super(message);
        }
    }
}
