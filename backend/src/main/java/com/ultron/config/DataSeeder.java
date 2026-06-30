package com.ultron.config;

import com.ultron.intelligence.trading.TradingRule;
import com.ultron.intelligence.trading.TradingRuleRepository;
import com.ultron.memory.MemoryRepository;
import com.ultron.memory.MemoryService;
import com.ultron.skills.SkillRepository;
import com.ultron.skills.SkillService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * First-run data seeder. When {@code ultron.seed.enabled=true} (the default in the running app, but
 * OFF in the test profile) and the relevant tables are empty, it preloads a handful of starter
 * skills, memories, and trading rules so a fresh install isn't a blank slate. Everything flows
 * through the normal services, so seeded skills/memories are embedded and searchable immediately.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final boolean enabled;
    private final MemoryService memoryService;
    private final SkillService skillService;
    private final MemoryRepository memoryRepository;
    private final SkillRepository skillRepository;
    private final TradingRuleRepository ruleRepository;

    public DataSeeder(@Value("${ultron.seed.enabled:false}") boolean enabled,
                      MemoryService memoryService, SkillService skillService,
                      MemoryRepository memoryRepository, SkillRepository skillRepository,
                      TradingRuleRepository ruleRepository) {
        this.enabled = enabled;
        this.memoryService = memoryService;
        this.skillService = skillService;
        this.memoryRepository = memoryRepository;
        this.skillRepository = skillRepository;
        this.ruleRepository = ruleRepository;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        try {
            seedSkills();
            seedMemories();
            seedRules();
        } catch (Exception ex) {
            log.warn("Data seeding skipped/partial: {}", ex.getMessage());
        }
    }

    private void seedSkills() {
        if (skillRepository.count() > 0) {
            return;
        }
        skillService.intake("Deploy frontend", "Ship the React control panel",
            "To deploy the frontend: run npm run build, then run netlify deploy --prod from the frontend directory.",
            "devops,frontend", "seed");
        skillService.intake("Tune pgvector", "Vector index performance",
            "For pgvector, build an ivfflat index and set lists ~= rows/1000; raise probes at query time to trade speed for recall.",
            "memory,performance", "seed");
        skillService.intake("Mark Douglas — trading discipline", "Core trading psychology",
            "Think in probabilities. Any single trade's outcome is random; your edge plays out over a series. "
                + "Predefine risk, accept it before entering, and never let one trade threaten the account.",
            "trading,psychology", "seed");
        skillService.intake("Morning routine", "Operator daily rhythm",
            "Mornings are for strategy and deep work. Review the overnight brief, pick the one most important thing, "
                + "and protect the first two focused hours from meetings and notifications.",
            "productivity", "seed");
        log.info("Seeded {} starter skills", skillRepository.count());
    }

    private void seedMemories() {
        if (memoryRepository.count() > 0) {
            return;
        }
        memoryService.save("My production AWS region is eu-west-1 for all stacks.", "NOTE", "seed", "infra,aws");
        memoryService.save("Daily trading loss limit is 2% of account; max 3 trades per day.", "NOTE", "seed", "trading,rules");
        memoryService.save("Prefer concise, direct answers. Chief-of-staff tone, not chatbot.", "PREFERENCE", "seed", "style");
        memoryService.save("Standup is 09:30 on weekdays; prep a GitHub commit summary beforehand.", "NOTE", "seed", "calendar");
        log.info("Seeded {} starter memories", memoryRepository.count());
    }

    private void seedRules() {
        if (ruleRepository.count() > 0) {
            return;
        }
        ruleRepository.save(new TradingRule(UUID.randomUUID(), "Risk-reward minimum",
            "Only take trades with a reward-to-risk ratio of at least 2:1.", "Mark Douglas — trading discipline", true));
        ruleRepository.save(new TradingRule(UUID.randomUUID(), "Daily loss limit",
            "Stop trading for the day after losing 2% of the account.", null, true));
        ruleRepository.save(new TradingRule(UUID.randomUUID(), "No revenge trades",
            "After two consecutive losses in an hour, step away and review with fresh eyes.", "Mark Douglas — trading discipline", true));
        ruleRepository.save(new TradingRule(UUID.randomUUID(), "Avoid the open",
            "Do not trade in the first 15 minutes after the market opens.", null, true));
        log.info("Seeded {} starter trading rules", ruleRepository.count());
    }
}
