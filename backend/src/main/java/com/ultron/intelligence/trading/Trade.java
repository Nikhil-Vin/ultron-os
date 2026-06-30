package com.ultron.intelligence.trading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A trade (paper or live) — Section 7. Default {@code executionMode} is paper. */
@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "instrument", nullable = false, length = 100)
    private String instrument;

    @Column(name = "trade_type", nullable = false, length = 10)
    private String tradeType; // BUY, SELL, SHORT, COVER

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "entry_price", precision = 18, scale = 4)
    private BigDecimal entryPrice;

    @Column(name = "exit_price", precision = 18, scale = 4)
    private BigDecimal exitPrice;

    @Column(name = "stop_loss", precision = 18, scale = 4)
    private BigDecimal stopLoss;

    @Column(name = "target", precision = 18, scale = 4)
    private BigDecimal target;

    @Column(name = "pnl", precision = 18, scale = 4)
    private BigDecimal pnl;

    @Column(name = "risk_reward", precision = 8, scale = 4)
    private BigDecimal riskReward;

    @Column(name = "signal_source")
    private String signalSource;

    @Column(name = "psychology_flags")
    private String psychologyFlags;

    @Column(name = "broker_order_id", length = 100)
    private String brokerOrderId;

    @Column(name = "execution_mode", nullable = false, length = 20)
    private String executionMode = "paper";

    @Column(name = "approved_by_voice", nullable = false)
    private boolean approvedByVoice = false;

    @Column(name = "entered_at")
    private Instant enteredAt;

    @Column(name = "exited_at")
    private Instant exitedAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Trade() {
    }

    public Trade(UUID id, String instrument, String tradeType, int quantity, BigDecimal entryPrice,
                 BigDecimal stopLoss, BigDecimal target, BigDecimal riskReward, String signalSource,
                 String psychologyFlags, String brokerOrderId, String executionMode, boolean approvedByVoice) {
        this.id = id;
        this.instrument = instrument;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.target = target;
        this.riskReward = riskReward;
        this.signalSource = signalSource;
        this.psychologyFlags = psychologyFlags;
        this.brokerOrderId = brokerOrderId;
        this.executionMode = executionMode;
        this.approvedByVoice = approvedByVoice;
        this.enteredAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getInstrument() { return instrument; }
    public String getTradeType() { return tradeType; }
    public int getQuantity() { return quantity; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public BigDecimal getExitPrice() { return exitPrice; }
    public BigDecimal getStopLoss() { return stopLoss; }
    public BigDecimal getTarget() { return target; }
    public BigDecimal getPnl() { return pnl; }
    public BigDecimal getRiskReward() { return riskReward; }
    public String getSignalSource() { return signalSource; }
    public String getPsychologyFlags() { return psychologyFlags; }
    public String getBrokerOrderId() { return brokerOrderId; }
    public String getExecutionMode() { return executionMode; }
    public boolean isApprovedByVoice() { return approvedByVoice; }
    public Instant getEnteredAt() { return enteredAt; }
    public Instant getExitedAt() { return exitedAt; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }

    public void setExitPrice(BigDecimal exitPrice) { this.exitPrice = exitPrice; }
    public void setPnl(BigDecimal pnl) { this.pnl = pnl; }
    public void setExitedAt(Instant exitedAt) { this.exitedAt = exitedAt; }
    public void setNotes(String notes) { this.notes = notes; }
}
