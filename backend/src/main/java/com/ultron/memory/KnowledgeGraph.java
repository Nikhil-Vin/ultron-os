package com.ultron.memory;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Knowledge graph (L4 — Section 9.1). Connects people, projects, concepts and topics across
 * domains so a concept from one skill can link to a pattern in another. Adjacency-list model over
 * Postgres; the service contract is Neo4j-swappable later.
 */
@Service
public class KnowledgeGraph {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraph.class);

    private final KgEntityRepository entities;
    private final KgRelationRepository relations;

    KnowledgeGraph(KgEntityRepository entities, KgRelationRepository relations) {
        this.entities = entities;
        this.relations = relations;
    }

    /** Get-or-create an entity by (name, type). */
    @Transactional
    public KgEntity upsertEntity(String name, String type) {
        String t = (type == null || type.isBlank()) ? "concept" : type;
        return entities.findByNameAndType(name, t)
            .orElseGet(() -> entities.save(new KgEntity(UUID.randomUUID(), name, t)));
    }

    /** Create a directed relation between two entities (created if needed). */
    @Transactional
    public KgRelation relate(String fromName, String fromType, String relation, String toName, String toType) {
        KgEntity from = upsertEntity(fromName, fromType);
        KgEntity to = upsertEntity(toName, toType);
        KgRelation rel = relations.save(new KgRelation(UUID.randomUUID(), from.getId(), to.getId(), relation));
        log.info("KG: {} -[{}]-> {}", fromName, relation, toName);
        return rel;
    }

    /** Outgoing + incoming neighbours of an entity. */
    @Transactional(readOnly = true)
    public Neighbours neighbours(String name, String type) {
        KgEntity e = entities.findByNameAndType(name, (type == null || type.isBlank()) ? "concept" : type)
            .orElse(null);
        if (e == null) {
            return new Neighbours(List.of(), List.of());
        }
        return new Neighbours(relations.findByFromId(e.getId()), relations.findByToId(e.getId()));
    }

    public record Neighbours(List<KgRelation> outgoing, List<KgRelation> incoming) {
    }
}
