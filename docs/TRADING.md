# Ultron-OS — Trading Module

> Paper trading is the wired default. **Live order placement is a gated, opt-in extension point you wire yourself.** No code path here moves real money on its own.

## What's real and working (paper, no money)
- **SignalGenerator** — structured BUY/SELL/HOLD + confidence from an indicator snapshot (RSI/MACD/sentiment).
- **TradingBrain** — fuses the signal + your ingested rules (skills) + RAG knowledge + the LLM into a graded, rule-checked read.
- **RiskCalculator** — Kelly criterion (capped 25%), fixed-fractional position sizing, R:R.
- **PreTradeChecklist** — R:R ≥ 2, session timing, daily-loss limit, psychology gate.
- **TradingPsychMonitor** — FOMO / revenge / overtrading / rule-violation flags + rolling discipline score.
- **TradeJournal** — logs paper trades via `PaperBroker`, computes win-rate / P&L / avg R:R.
- **ai-layer/trading/** — TA-Lib indicators, FinBERT sentiment, signal fusion, Kelly, VectorBT backtester, Black-Scholes greeks, candlestick CNN (all with offline fallbacks).

## Endpoints (`/api/trading/*`)
`status`, `quote`, `signal`, `signals`, `risk`, `checklist`, `psychology`, `paper-trade`, `execute`, `journal`, `rules`.

## Execution & the moat
- `execute` is CRITICAL: it requires a passing **voice biometric** (`VoiceIdGate`). On success it places a **paper** trade and tells you live execution isn't wired — exactly the Phase 3 acceptance test.
- `BrokerConnector.placeOrder()` for `ZerodhaBroker`/`AlpacaBroker` **throws `LiveExecutionNotWiredException`**. Wiring it live is a deliberate step you take:
  1. Add the broker SDK + API keys to env (never in code).
  2. Implement the real `placeOrder(...)`.
  3. Gate it behind an `EXECUTION_MODE=live` flag you set yourself.

## Wire live (when you're ready, at your own risk)
Live trading involves real financial risk. Ultron will warn, size, checklist, and psychology-check — but the decision and the wiring are yours. Start with the broker's paper API before going live.
