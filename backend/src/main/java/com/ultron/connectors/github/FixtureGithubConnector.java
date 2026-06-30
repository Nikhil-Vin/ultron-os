package com.ultron.connectors.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Offline GitHub connector (L5). Reads {@code fixtures/github.sample.json} from the classpath.
 * Active by default ({@code ultron.github.mode=fixture}) — no network, no token, fully offline.
 */
@Component
@ConditionalOnProperty(prefix = "ultron.github", name = "mode", havingValue = "fixture", matchIfMissing = true)
public class FixtureGithubConnector implements GithubConnector {

    private static final Logger log = LoggerFactory.getLogger(FixtureGithubConnector.class);
    private static final String FIXTURE = "fixtures/github.sample.json";

    private final ObjectMapper objectMapper;

    public FixtureGithubConnector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("GitHub connector: FIXTURE mode (offline)");
    }

    @Override
    public String mode() {
        return "fixture";
    }

    @Override
    public GithubSnapshot snapshot() {
        try (InputStream in = new ClassPathResource(FIXTURE).getInputStream()) {
            return objectMapper.readValue(in, GithubSnapshot.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load GitHub fixture " + FIXTURE, ex);
        }
    }
}
