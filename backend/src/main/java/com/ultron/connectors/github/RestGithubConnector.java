package com.ultron.connectors.github;

import com.ultron.config.UltronProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Opt-in REST GitHub connector (L5). Active only when {@code ultron.github.mode=rest}.
 *
 * <p>Phase 0 keeps this as a guarded stub: the live REST/GraphQL calls (requiring a
 * {@code GITHUB_TOKEN}) are wired in a later phase. It is intentionally NOT the default,
 * so the system stays fully offline out of the box.
 */
@Component
@ConditionalOnProperty(prefix = "ultron.github", name = "mode", havingValue = "rest")
public class RestGithubConnector implements GithubConnector {

    private static final Logger log = LoggerFactory.getLogger(RestGithubConnector.class);

    private final UltronProperties properties;

    public RestGithubConnector(UltronProperties properties) {
        this.properties = properties;
        log.info("GitHub connector: REST mode (live API wiring lands in a later phase)");
    }

    @Override
    public String mode() {
        return "rest";
    }

    @Override
    public GithubSnapshot snapshot() {
        if (properties.getGithub().getToken() == null || properties.getGithub().getToken().isBlank()) {
            throw new IllegalStateException(
                "GitHub REST mode requires GITHUB_TOKEN. Set it or use ULTRON_GITHUB_MODE=fixture.");
        }
        // Live calls deferred to a later phase; return an empty-but-valid snapshot for now.
        return new GithubSnapshot(
            java.time.Instant.now().toString(),
            "Nikhil-Vin",
            List.of());
    }
}
