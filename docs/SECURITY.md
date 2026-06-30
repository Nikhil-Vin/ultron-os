# Ultron-OS — Security & Governance

## Risk tiers (Section 14)
| Level | Examples | Gate | Audit | Voice biometric |
|---|---|---|---|---|
| READ | query memory, view brief, signals, quotes | none | log | no |
| LOW | save memory, paper trade, learn skill, drafts | auto + notify | full | no |
| HIGH | send email, post Slack, calendar create, scenes, push | approval gate | full | no |
| CRITICAL | live trade, SMS/call, delete, spend | gate + voice | immutable | **yes** |

`ULTRON_AUTO_APPROVE=false` is the hardcoded default. CRITICAL actions are blocked until an explicit, voice-verified "go".

## The moat (never violated)
- **Live money / live contact code ships disabled.** `ZerodhaBroker`/`AlpacaBroker.placeOrder()` and `TwilioConnector.sendSms()` throw / refuse until the owner deliberately wires + enables them. Paper trading is the wired default. Verified by `BrokerMoatTest`.
- **Local-first.** Data, models, embeddings, memory live on your hardware. Nothing leaves without per-action consent.
- **Zero secrets in code.** All tokens are env vars (`ConnectorProperties`, `PythonBridgeConfig`); Gitleaks + OWASP dependency-check run in CI.

## Controls implemented
- `ApprovalGate` + append-only `AuditLog` (every gated action recorded).
- `VoiceIdGate` — Resemblyzer cosine ≥ threshold required for CRITICAL; **blocked if no voiceprint enrolled** (secure default).
- `InputSanitizer` — strips control chars, caps length, flags prompt-injection; all untrusted text (webhooks, ingest) passes through it.
- `RateLimiter` — per-key sliding window on ingress.
- Device agents enforce a **capability manifest**; HIGH+ actions require a backend approval token; forbidden actions always refused; mTLS device certs.
- CORS allow-list (no wildcards). Spring Security in front of `/api/**`.

## Reporting
This is a solo-operator system. Treat your `.env`, voiceprint, and device certs as the crown jewels — they are the only keys to CRITICAL actions.
