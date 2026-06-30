package com.ultron.memory;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link Memory} (L4).
 *
 * <p>Phase 0 recall is keyword-based (case-insensitive LIKE over content + tags) — this is
 * the fail-safe fallback that remains even after pgvector semantic search lands in Phase 1.
 */
public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    @Query("""
        select m from Memory m
        where lower(m.content) like lower(concat('%', :q, '%'))
           or lower(coalesce(m.tags, '')) like lower(concat('%', :q, '%'))
        order by m.createdAt desc
        """)
    List<Memory> searchByKeyword(@Param("q") String query, Pageable pageable);

    List<Memory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
