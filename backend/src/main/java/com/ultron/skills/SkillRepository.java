package com.ultron.skills;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link Skill} (L4). Keyword search mirrors the memory fallback so skill recall
 * still works when no embedder is available.
 */
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    @Query("""
        select s from Skill s
        where lower(s.name) like lower(concat('%', :q, '%'))
           or lower(s.content) like lower(concat('%', :q, '%'))
           or lower(coalesce(s.description, '')) like lower(concat('%', :q, '%'))
           or lower(coalesce(s.tags, '')) like lower(concat('%', :q, '%'))
        order by s.createdAt desc
        """)
    List<Skill> searchByKeyword(@Param("q") String query, Pageable pageable);

    List<Skill> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Only active skills for RAG retrieval (excludes paused/archived). */
    @Query("select s from Skill s where s.status = 'active' order by s.createdAt desc")
    List<Skill> findActiveSkills(Pageable pageable);
}
