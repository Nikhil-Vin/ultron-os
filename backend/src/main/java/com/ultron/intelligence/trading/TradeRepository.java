package com.ultron.intelligence.trading;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
    List<Trade> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Trade> findByExecutionModeOrderByCreatedAtDesc(String executionMode, Pageable pageable);
}
