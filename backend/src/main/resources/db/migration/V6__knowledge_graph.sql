-- =============================================================================
-- V6__knowledge_graph.sql — Ultron-OS Phase 3: Knowledge Graph (L4)
--   Entity–relation store connecting people, projects, concepts, topics across
--   domains (the "idea graph" from Section 9.1). Lightweight adjacency model in
--   Postgres; can migrate to Neo4j later without changing the service contract.
-- =============================================================================

CREATE TABLE IF NOT EXISTS kg_entities (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL DEFAULT 'concept', -- person | project | concept | topic
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (name, type)
);

CREATE TABLE IF NOT EXISTS kg_relations (
    id          UUID PRIMARY KEY,
    from_id     UUID NOT NULL REFERENCES kg_entities(id) ON DELETE CASCADE,
    to_id       UUID NOT NULL REFERENCES kg_entities(id) ON DELETE CASCADE,
    relation    VARCHAR(100) NOT NULL,                   -- e.g. relates_to, depends_on, authored_by
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_kg_relations_from ON kg_relations (from_id);
CREATE INDEX IF NOT EXISTS idx_kg_relations_to   ON kg_relations (to_id);
CREATE INDEX IF NOT EXISTS idx_kg_entities_type  ON kg_entities (type);
