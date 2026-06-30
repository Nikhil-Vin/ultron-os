-- =============================================================================
-- V5__trading.sql — Ultron-OS Phase 3: Trading module (Section 7)
--   Paper trading is the wired default. The live broker order path is a gated,
--   opt-in extension point — no schema here enables real money on its own.
-- =============================================================================

CREATE TABLE IF NOT EXISTS trading_rules (
    id              UUID PRIMARY KEY,
    rule_name       VARCHAR(255) NOT NULL,
    rule_text       TEXT NOT NULL,
    source_skill_tag VARCHAR(100),         -- which ingested book/PDF this came from
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS trades (
    id              UUID PRIMARY KEY,
    instrument      VARCHAR(100) NOT NULL,   -- NIFTY50, RELIANCE, BTC/USD
    trade_type      VARCHAR(10)  NOT NULL,   -- BUY, SELL, SHORT, COVER
    quantity        INTEGER NOT NULL,
    entry_price     DECIMAL(18,4),
    exit_price      DECIMAL(18,4),
    stop_loss       DECIMAL(18,4),
    target          DECIMAL(18,4),
    pnl             DECIMAL(18,4),
    risk_reward     DECIMAL(8,4),
    signal_source   TEXT,
    psychology_flags TEXT,                   -- comma-separated: FOMO, revenge_trade, rule_violation
    broker_order_id VARCHAR(100),
    execution_mode  VARCHAR(20) NOT NULL DEFAULT 'paper', -- paper | live
    approved_by_voice BOOLEAN NOT NULL DEFAULT FALSE,
    entered_at      TIMESTAMPTZ,
    exited_at       TIMESTAMPTZ,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS trading_signals (
    id              UUID PRIMARY KEY,
    instrument      VARCHAR(100) NOT NULL,
    signal_type     VARCHAR(20)  NOT NULL,   -- BUY, SELL, HOLD
    confidence      DECIMAL(5,4),
    reasoning       TEXT,
    indicator_snapshot TEXT,                 -- JSON of RSI/MACD/BB at signal time
    sentiment_score DECIMAL(5,4),
    acted_on        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_trades_created_at  ON trades (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_trades_instrument  ON trades (instrument);
CREATE INDEX IF NOT EXISTS idx_signals_created_at ON trading_signals (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_rules_active       ON trading_rules (active);
