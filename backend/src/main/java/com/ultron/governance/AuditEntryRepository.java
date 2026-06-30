package com.ultron.governance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {

    List<AuditEntry> findTop50ByOrderByCreatedAtDesc();
}
