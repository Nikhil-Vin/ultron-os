package com.ultron.intelligence.trading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A generated trading signal (logged whether or not it's acted on) — Section 7. */
@Entity
@Table(name = "trading_signals")
public class TradingSignal {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "instrument", nullable = false, length = 100)
    private String instrument;

    @Column(name = "signal_type", nullable = false, length = 20)
    private String signalType; // BUY, SELL, HOLD

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "reasoning")
    private String reasoning;

    @Column(name = "indicator_snapshot")
    private String indicatorSnapshot; // JSON

    @Column(name = "sentiment_score", precision = 5, scale = 4)
    private BigDecimal sentimentScore;

    @Column(name = "acted_on", nullable = false)
    private boolean actedOn = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected TradingSignal() {
    }

    public TradingSignal(UUID id, String instrument, String signalType, BigDecimal confidence,
                         String reasoning, String indicatorSnapshot, BigDecimal sentimentScore) {
        this.id = id;
        this.instrument = instrument;
        this.signalType = signalType;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.indicatorSnapshot = indicatorSnapshot;
        this.sentimentScore = sentimentScore;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getInstrument() { return instrument; }
    public String getSignalType() { return signalType; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public String getIndicatorSnapshot() { return indicatorSnapshot; }
    public BigDecimal getSentimentScore() { return sentimentScore; }
    public boolean isActedOn() { return actedOn; }
    public Instant getCreatedAt() { return createdAt; }

    public void setActedOn(boolean actedOn) { this.actedOn = actedOn; }
}
