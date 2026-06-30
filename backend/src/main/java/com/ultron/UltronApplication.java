package com.ultron;

import com.ultron.config.UltronProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ultron-OS — the brain server.
 *
 * <p>Phase 0 boots the L1–L6 skeleton: kernel + scheduler, workers, an offline-first
 * brain, a growing memory, the GitHub fixture connector, and the governance gate.
 * It runs fully offline with zero paid API keys.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(UltronProperties.class)
public class UltronApplication {

    public static void main(String[] args) {
        SpringApplication.run(UltronApplication.class, args);
    }
}
