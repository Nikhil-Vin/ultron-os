-- =============================================================================
-- V1__init.sql — Ultron-OS Phase 0 base schema
-- Enables pgvector (for later phases), and creates the two foundational tables:
--   memories   (L4 — The Moat: growing, searchable memory)
--   audit_log  (L6 — The Conscience: append-only record of every gated action)
-- Embedding columns + skills registry arrive in V2 (Phase 1).
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- ----------------------------------------------------------------------------
-- L4 MEMORY
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS memories (
    id          UUID PRIMARY KEY,
    content     TEXT        NOT NULL,
    type        VARCHAR(50) NOT NULL DEFAULT 'NOTE',
    source      VARCHAR(100),
    tags        TEXT,                      -- comma-separated tags (normalised in P1)
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_memories_created_at ON memories (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_memories_type       ON memories (type);

-- ----------------------------------------------------------------------------
-- L6 GOVERNANCE — append-only audit log
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id           UUID PRIMARY KEY,
    action       VARCHAR(255) NOT NULL,    -- what was attempted
    risk_level   VARCHAR(20)  NOT NULL,    -- READ / LOW / HIGH / CRITICAL
    decision     VARCHAR(20)  NOT NULL,    -- APPROVED / DENIED / AUTO
    actor        VARCHAR(100) NOT NULL DEFAULT 'system',
    detail       TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_risk_level ON audit_log (risk_level);
