package com.ultron.intelligence.finance;

import com.ultron.intelligence.trading.TradeJournal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Financial overview (Section 9.7). Aggregates trading P&L (from the {@link TradeJournal}) with
 * holdings + spending the owner supplies (bank/portfolio APIs are opt-in connectors), into a single
 * net-worth-style snapshot. READ-only analysis.
 */
@Service
public class FinancialOverviewService {

    private final TradeJournal journal;

    public FinancialOverviewService(TradeJournal journal) {
        this.journal = journal;
    }

    /**
     * Build an overview. Holdings + monthly spend are passed in (from connectors or manual entry);
     * trading P&L is read live from the journal.
     *
     * @param holdings     current investment/cash holdings
     * @param monthlySpend total spend this month
     * @param budget       monthly budget
     */
    public Overview overview(List<Holding> holdings, double monthlySpend, double budget) {
        double holdingsTotal = holdings == null ? 0.0 : holdings.stream().mapToDouble(Holding::value).sum();
        double tradingPnl = journal.performance().totalPnl();
        double netWorth = holdingsTotal + tradingPnl;
        double budgetRemaining = budget - monthlySpend;
        return new Overview(netWorth, holdingsTotal, tradingPnl, monthlySpend, budget, budgetRemaining,
            holdings == null ? List.of() : holdings);
    }

    public record Holding(String name, double value) {
    }

    public record Overview(double netWorth, double holdingsTotal, double tradingPnl,
                           double monthlySpend, double budget, double budgetRemaining,
                           List<Holding> holdings) {
    }
}
