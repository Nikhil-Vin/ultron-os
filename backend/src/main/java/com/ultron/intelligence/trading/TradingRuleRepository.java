package com.ultron.intelligence.trading;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingRuleRepository extends JpaRepository<TradingRule, UUID> {
    List<TradingRule> findByActiveTrueOrderByCreatedAtDesc();
}
