-- =============================================================================
-- V2__rag_and_skills.sql — Ultron-OS Phase 1: RAG + Skill Intake
--   • memories.embedding  — CSV-encoded dense vector for semantic recall (L3 RAG).
--   • skills               — learned-skill registry (L4 Moat), also embedded.
--
-- Embeddings are stored as TEXT (CSV) so retrieval is portable and fully offline:
-- ranking is computed in Java (cosine). pgvector (enabled in V1) remains the drop-in
-- production accelerator; wiring native vector search is a later optimisation.
-- =============================================================================

-- ----------------------------------------------------------------------------
-- L4 MEMORY — add semantic embedding
-- ----------------------------------------------------------------------------
ALTER TABLE memories ADD COLUMN IF NOT EXISTS embedding TEXT;

-- ----------------------------------------------------------------------------
-- L4 SKILLS — the learned-skill registry
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS skills (
    id          UUID PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    content     TEXT          NOT NULL,
    tags        TEXT,
    source      VARCHAR(100),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    embedding   TEXT
);

CREATE INDEX IF NOT EXISTS idx_skills_created_at ON skills (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_skills_name       ON skills (name);
