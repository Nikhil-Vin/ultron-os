package com.ultron.connectors.github;

import java.util.List;

/**
 * Immutable snapshot of GitHub activity used to build the overnight brief (L5).
 * Mirrors the shape of {@code fixtures/github.sample.json}.
 */
public record GithubSnapshot(
    String generatedAt,
    String owner,
    List<Repository> repositories) {

    public record Repository(
        String name,
        List<PullRequest> openPullRequests,
        List<Commit> recentCommits,
        List<Issue> openIssues) {
    }

    public record PullRequest(
        int number,
        String title,
        String author,
        String updatedAt,
        String reviewState) {
    }

    public record Commit(
        String sha,
        String message,
        String author,
        String committedAt) {
    }

    public record Issue(
        int number,
        String title,
        List<String> labels,
        String updatedAt) {
    }
}
