package com.ultron.intelligence.trading;

import com.ultron.connectors.broker.BrokerConnector;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.governance.VoiceIdGate;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trading API (L0 — Section 7). Real-time advice, signals, risk, checklist, paper trading, journal,
 * and rules. Live execution is gated: it requires a passing {@link VoiceIdGate} AND a deliberately
 * wired live broker — neither of which ships enabled, so "execute" results in a PAPER trade with a
 * clear note, exactly per the Phase 3 acceptance test.
 */
@RestController
@RequestMapping("/api/trading")
public class TradingController {

    private final TradingBrain tradingBrain;
    private final SignalGenerator signalGenerator;
    private final TradingSignalRepository signalRepo;
    private final RiskCalculator riskCalculator;
    private final PreTradeChecklist checklist;
    private final TradingPsychMonitor psychMonitor;
    private final TradeJournal journal;
    private final TradingRuleRepository rules;
    private final BrokerConnector broker;        // @Primary paper
    private final List<BrokerConnector> brokers; // all, for status
    private final VoiceIdGate voiceIdGate;

    public TradingController(TradingBrain tradingBrain, SignalGenerator signalGenerator,
                             TradingSignalRepository signalRepo, RiskCalculator riskCalculator,
                             PreTradeChecklist checklist, TradingPsychMonitor psychMonitor,
                             TradeJournal journal, TradingRuleRepository rules,
                             BrokerConnector broker, List<BrokerConnector> brokers, VoiceIdGate voiceIdGate) {
        this.tradingBrain = tradingBrain;
        this.signalGenerator = signalGenerator;
        this.signalRepo = signalRepo;
        this.riskCalculator = riskCalculator;
        this.checklist = checklist;
        this.psychMonitor = psychMonitor;
        this.journal = journal;
        this.rules = rules;
        this.broker = broker;
        this.brokers = brokers;
        this.voiceIdGate = voiceIdGate;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("defaultBroker", broker.name());
        body.put("brokers", brokers.stream().map(b -> Map.of(
            "name", b.name(), "mode", b.mode(), "liveWired", b.isLiveWired())).toList());
        body.put("note", "Paper trading is the default. Live execution is a gated, opt-in extension point.");
        return body;
    }

    @GetMapping("/quote")
    public BrokerConnector.Quote quote(@RequestParam String instrument) {
        return broker.quote(instrument);
    }

    @PostMapping("/signal")
    public TradingBrain.Advice signal(@RequestBody SignalRequest req) {
        return tradingBrain.advise(req.instrument(), req.indicators() == null ? Map.of() : req.indicators());
    }

    @GetMapping("/signals")
    public List<TradingSignal> signals() {
        return signalRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50));
    }

    @PostMapping("/risk")
    public RiskCalculator.Sizing risk(@RequestBody RiskRequest req) {
        return riskCalculator.recommend(req.accountValue(), req.riskFraction(), req.entry(), req.stop(), req.target());
    }

    @PostMapping("/checklist")
    public PreTradeChecklist.Result checklist(@RequestBody ChecklistRequest req) {
        TradingPsychMonitor.Assessment psych = psychMonitor.assess(req.missedInitialMove(), req.matchesRule());
        return checklist.run(req.entry(), req.stop(), req.target(), req.dailyPnl(),
            req.dailyLossLimit(), psych, LocalTime.now());
    }

    @GetMapping("/psychology")
    public TradingPsychMonitor.Assessment psychology(
        @RequestParam(defaultValue = "false") boolean missedInitialMove,
        @RequestParam(defaultValue = "true") boolean matchesRule) {
        return psychMonitor.assess(missedInitialMove, matchesRule);
    }

    @PostMapping("/paper-trade")
    public Trade paperTrade(@RequestBody PaperTradeRequest req) {
        return journal.paperTrade(req.instrument(), req.side(), req.quantity(),
            req.stop(), req.target(), req.signalSource(), req.psychologyFlags());
    }

    /**
     * "Execute" path (CRITICAL). Requires a passing voice biometric. Because live broker execution
     * is a deliberately-unwired extension point, a verified execute results in a PAPER trade with a
     * clear note — never a real order (Phase 3 acceptance test).
     */
    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody ExecuteRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        VoiceIdGate.Result vid = voiceIdGate.verify(
            new ProposedAction("trading.execute", RiskLevel.CRITICAL,
                req.side() + " " + req.quantity() + " " + req.instrument(), "trading"),
            req.similarity());
        body.put("voiceId", Map.of("passed", vid.passed(), "reason", vid.reason(), "auditId", vid.auditId()));
        if (!vid.passed()) {
            body.put("executed", false);
            body.put("message", "Blocked: voice verification did not pass. CRITICAL action requires your verified voice.");
            return body;
        }
        // Voice OK, but live execution is not wired → log as paper, say so plainly.
        boolean anyLiveWired = brokers.stream().anyMatch(BrokerConnector::isLiveWired);
        Trade trade = journal.paperTrade(req.instrument(), req.side(), req.quantity(),
            req.stop(), req.target(), "voice-execute", null);
        body.put("executed", true);
        body.put("executionMode", "paper");
        body.put("tradeId", trade.getId().toString());
        body.put("message", anyLiveWired
            ? "Voice confirmed. A live broker is wired, but Ultron still defaults to paper unless EXECUTION_MODE=live is set deliberately."
            : "Voice confirmed. Live execution isn't wired, so this was placed as a PAPER trade (no real money moved).");
        return body;
    }

    @GetMapping("/journal")
    public Map<String, Object> journal() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trades", journal.recent(50));
        body.put("performance", journal.performance());
        return body;
    }

    @GetMapping("/rules")
    public List<TradingRule> listRules() {
        return rules.findByActiveTrueOrderByCreatedAtDesc();
    }

    @PostMapping("/rules")
    public TradingRule addRule(@RequestBody RuleRequest req) {
        return rules.save(new TradingRule(UUID.randomUUID(), req.ruleName(), req.ruleText(),
            req.sourceSkillTag(), true));
    }

    // --- DTOs ---
    public record SignalRequest(@NotBlank String instrument, Map<String, Double> indicators) {
    }

    public record RiskRequest(double accountValue, double riskFraction, double entry, double stop, double target) {
    }

    public record ChecklistRequest(double entry, double stop, double target, double dailyPnl,
                                   double dailyLossLimit, boolean missedInitialMove, boolean matchesRule) {
    }

    public record PaperTradeRequest(@NotBlank String instrument, @NotBlank String side, int quantity,
                                    BigDecimal stop, BigDecimal target, String signalSource, String psychologyFlags) {
    }

    public record ExecuteRequest(@NotBlank String instrument, @NotBlank String side, int quantity,
                                 BigDecimal stop, BigDecimal target, Double similarity) {
    }

    public record RuleRequest(@NotBlank String ruleName, @NotBlank String ruleText, String sourceSkillTag) {
    }
}
