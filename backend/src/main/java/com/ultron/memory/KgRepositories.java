package com.ultron.memory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface KgEntityRepository extends JpaRepository<KgEntity, UUID> {
    Optional<KgEntity> findByNameAndType(String name, String type);
}

interface KgRelationRepository extends JpaRepository<KgRelation, UUID> {
    List<KgRelation> findByFromId(UUID fromId);
    List<KgRelation> findByToId(UUID toId);
}
