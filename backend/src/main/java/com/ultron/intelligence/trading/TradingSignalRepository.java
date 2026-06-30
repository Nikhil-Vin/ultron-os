package com.ultron.intelligence.trading;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingSignalRepository extends JpaRepository<TradingSignal, UUID> {
    List<TradingSignal> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
