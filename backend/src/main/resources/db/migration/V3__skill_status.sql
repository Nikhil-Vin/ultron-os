-- =============================================================================
-- V3__skill_status.sql — Ultron-OS Phase 1: skill lifecycle status
--   Adds a status column to the skills registry so a skill can be paused
--   (temporarily excluded from search + RAG) or archived without deletion,
--   per Section 6's skill management endpoints.
-- =============================================================================

ALTER TABLE skills ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';

CREATE INDEX IF NOT EXISTS idx_skills_status ON skills (status);
