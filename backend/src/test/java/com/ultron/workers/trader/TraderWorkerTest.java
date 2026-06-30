package com.ultron.workers.trader;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.config.UltronProperties;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.AuditEntryRepository;
import com.ultron.governance.AuditLog;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Trader risk-tier behaviour on in-memory H2. Confirms Section 14 gating: paper trades auto-run,
 * live trades are blocked by default (human-in-the-loop), and watch is a free READ.
 */
@DataJpaTest
class TraderWorkerTest {

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private TraderWorker trader;

    @BeforeEach
    void setUp() {
        UltronProperties props = new UltronProperties(); // auto-approve = false
        ApprovalGate gate = new ApprovalGate(props, new AuditLog(auditEntryRepository));
        trader = new TraderWorker(gate);
    }

    @Test
    void watchIsAFreeRead() {
        WorkerResult result = trader.handle(WorkerRequest.of("watch"));
        assertThat(result.success()).isTrue();
        assertThat(result.summary()).contains("Watchlist:");
    }

    @Test
    void paperTradeAutoApproves() {
        WorkerResult result = trader.handle(
            new WorkerRequest("paper-trade", Map.of("symbol", "AAPL", "side", "buy", "qty", "10")));
        assertThat(result.success()).isTrue();
        assertThat(result.summary()).contains("simulated");
        assertThat(result.detail()).contains("decision=AUTO");
    }

    @Test
    void liveTradeIsBlockedByDefault() {
        WorkerResult result = trader.handle(
            new WorkerRequest("live-trade", Map.of("symbol", "AAPL", "side", "buy", "qty", "10")));
        assertThat(result.success()).isFalse();
        assertThat(result.summary()).contains("BLOCKED");
        assertThat(result.summary()).contains("decision=DENIED");
    }

    @Test
    void liveTradeProceedsWithExplicitApproval() {
        WorkerResult result = trader.handle(
            new WorkerRequest("live-trade", Map.of("symbol", "AAPL", "side", "buy", "qty", "10", "approved", "true")));
        assertThat(result.success()).isTrue();
        assertThat(result.detail()).contains("decision=APPROVED");
    }
}
