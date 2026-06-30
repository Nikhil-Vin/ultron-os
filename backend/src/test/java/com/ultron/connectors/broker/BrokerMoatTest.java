package com.ultron.connectors.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ultron.connectors.broker.BrokerConnector.OrderRequest;
import org.junit.jupiter.api.Test;

/**
 * Locks the Section 7 moat: the paper broker fills simulated orders; the live brokers refuse to
 * place real orders until the owner deliberately wires them.
 */
class BrokerMoatTest {

    @Test
    void paperBrokerFillsSimulatedOrders() {
        PaperBroker paper = new PaperBroker();
        assertThat(paper.isLiveWired()).isFalse();
        BrokerConnector.OrderResult r = paper.placeOrder(new OrderRequest("NIFTY50", "BUY", 50, null, "MARKET"));
        assertThat(r.status()).isEqualTo("FILLED");
        assertThat(r.executionMode()).isEqualTo("paper");
        assertThat(r.filledPrice()).isNotNull();
    }

    @Test
    void zerodhaLiveOrderIsNotWired() {
        ZerodhaBroker z = new ZerodhaBroker();
        assertThat(z.isLiveWired()).isFalse();
        assertThatThrownBy(() -> z.placeOrder(new OrderRequest("NIFTY50", "BUY", 50, null, "MARKET")))
            .isInstanceOf(BrokerConnector.LiveExecutionNotWiredException.class);
    }

    @Test
    void alpacaLiveOrderIsNotWired() {
        AlpacaBroker a = new AlpacaBroker();
        assertThat(a.isLiveWired()).isFalse();
        assertThatThrownBy(() -> a.placeOrder(new OrderRequest("AAPL", "BUY", 10, null, "MARKET")))
            .isInstanceOf(BrokerConnector.LiveExecutionNotWiredException.class);
    }
}
