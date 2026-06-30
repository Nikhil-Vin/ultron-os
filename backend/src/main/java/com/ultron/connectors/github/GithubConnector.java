package com.ultron.connectors.github;

/**
 * Source of GitHub activity (L5 — The Hands). Phase 0 ships a fixture-backed implementation
 * (offline, no token); a REST implementation is opt-in for later phases.
 */
public interface GithubConnector {

    /** Connector mode reported by {@code /api/health}: {@code fixture} or {@code rest}. */
    String mode();

    /** Pull the current activity snapshot. */
    GithubSnapshot snapshot();
}
