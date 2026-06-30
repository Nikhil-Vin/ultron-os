package com.ultron.intelligence.trading;

import com.ultron.connectors.broker.BrokerConnector;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trade journal (Section 7). Records trades, runs them through the {@link PaperBroker} (the
 * {@code @Primary} default), and computes performance analytics. Paper trades are LOW risk →
 * auto-approved + audited; they never touch real money.
 */
@Service
public class TradeJournal {

    private static final Logger log = LoggerFactory.getLogger(TradeJournal.class);

    private final TradeRepository repository;
    private final BrokerConnector broker; // @Primary PaperBroker
    private final ApprovalGate approvalGate;

    public TradeJournal(TradeRepository repository, BrokerConnector broker, ApprovalGate approvalGate) {
        this.repository = repository;
        this.broker = broker;
        this.approvalGate = approvalGate;
    }

    /**
     * Execute + log a PAPER trade. Gated as LOW (auto + audited). Returns the persisted trade.
     */
    @Transactional
    public Trade paperTrade(String instrument, String side, int quantity, BigDecimal stop,
                            BigDecimal target, String signalSource, String psychologyFlags) {
        ProposedAction action = new ProposedAction(
            "trading.paper-trade", RiskLevel.LOW,
            side + " " + quantity + " " + instrument + " (paper)", "trade-journal");
        approvalGate.evaluate(action);

        BrokerConnector.OrderResult result = broker.placeOrder(
            new BrokerConnector.OrderRequest(instrument, side, quantity, null, "MARKET"));

        BigDecimal entry = result.filledPrice();
        BigDecimal rr = computeRr(entry, stop, target);
        Trade trade = new Trade(
            UUID.randomUUID(), instrument, side.toUpperCase(), quantity, entry,
            stop, target, rr, signalSource, psychologyFlags,
            result.orderId(), "paper", false);
        Trade saved = repository.save(trade);
        log.info("Paper trade logged id={} {} {} x{} @ {}", saved.getId(), side, instrument, quantity, entry);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Trade> recent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit > 0 ? limit : 50));
    }

    /** Performance analytics over all trades (PyFolio-style basics, computed in-JVM). */
    @Transactional(readOnly = true)
    public Performance performance() {
        List<Trade> all = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1000));
        int total = all.size();
        long closed = all.stream().filter(t -> t.getPnl() != null).count();
        long wins = all.stream().filter(t -> t.getPnl() != null && t.getPnl().signum() > 0).count();
        double totalPnl = all.stream().filter(t -> t.getPnl() != null)
            .mapToDouble(t -> t.getPnl().doubleValue()).sum();
        double avgRr = all.stream().filter(t -> t.getRiskReward() != null)
            .mapToDouble(t -> t.getRiskReward().doubleValue()).average().orElse(0.0);
        double winRate = closed == 0 ? 0.0 : (double) wins / closed;
        return new Performance(total, (int) closed, (int) wins, winRate, totalPnl, avgRr);
    }

    private static BigDecimal computeRr(BigDecimal entry, BigDecimal stop, BigDecimal target) {
        if (entry == null || stop == null || target == null) {
            return null;
        }
        double risk = Math.abs(entry.doubleValue() - stop.doubleValue());
        if (risk <= 0) {
            return null;
        }
        double rr = Math.abs(target.doubleValue() - entry.doubleValue()) / risk;
        return BigDecimal.valueOf(rr).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * @param totalTrades all logged trades
     * @param closedTrades trades with a realized P&L
     * @param wins        profitable closed trades
     * @param winRate     wins / closed
     * @param totalPnl    summed realized P&L
     * @param avgRiskReward average planned R:R
     */
    public record Performance(int totalTrades, int closedTrades, int wins, double winRate,
                              double totalPnl, double avgRiskReward) {
    }
}
