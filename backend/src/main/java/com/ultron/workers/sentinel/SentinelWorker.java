package com.ultron.workers.sentinel;

import com.ultron.connectors.github.GithubConnector;
import com.ultron.connectors.github.GithubSnapshot;
import com.ultron.intelligence.BrainSelector;
import com.ultron.workers.Worker;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sentinel (L2) — produces the overnight / morning brief (Section 9.8). Phase 0 reads the
 * GitHub snapshot and renders a readable summary of PRs awaiting review, recent commits and
 * open issues, then adds a one-line reasoned insight from the active brain.
 *
 * <p>This is a READ-level operation: it never mutates anything, so it runs freely.
 */
@Component
public class SentinelWorker implements Worker {

    private static final Logger log = LoggerFactory.getLogger(SentinelWorker.class);

    private final GithubConnector github;
    private final BrainSelector brain;

    public SentinelWorker(GithubConnector github, BrainSelector brain) {
        this.github = github;
        this.brain = brain;
    }

    @Override
    public String name() {
        return "sentinel";
    }

    @Override
    public WorkerResult handle(WorkerRequest request) {
        GithubSnapshot snapshot = github.snapshot();
        String brief = render(snapshot);
        log.info("Sentinel brief rendered for owner={} repos={}",
            snapshot.owner(), snapshot.repositories().size());
        return WorkerResult.ok(brief, "github-mode=" + github.mode());
    }

    /** Render a human-readable brief from the snapshot. */
    public String render(GithubSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("Good morning. Overnight brief for ").append(snapshot.owner()).append(":\n");

        List<GithubSnapshot.Repository> repos = snapshot.repositories();
        if (repos.isEmpty()) {
            sb.append("  No repository activity to report.\n");
        }

        int prCount = 0;
        for (GithubSnapshot.Repository repo : repos) {
            sb.append("\n• ").append(repo.name()).append('\n');

            List<GithubSnapshot.PullRequest> prs = repo.openPullRequests();
            prCount += prs.size();
            sb.append("  Open PRs (").append(prs.size()).append("):\n");
            for (GithubSnapshot.PullRequest pr : prs) {
                sb.append("    #").append(pr.number()).append(' ')
                  .append(pr.title())
                  .append("  [").append(pr.reviewState()).append("]\n");
            }

            sb.append("  Recent commits (").append(repo.recentCommits().size()).append("):\n");
            for (GithubSnapshot.Commit commit : repo.recentCommits()) {
                sb.append("    ").append(shortSha(commit.sha())).append(' ')
                  .append(commit.message()).append('\n');
            }

            sb.append("  Open issues (").append(repo.openIssues().size()).append("):\n");
            for (GithubSnapshot.Issue issue : repo.openIssues()) {
                sb.append("    #").append(issue.number()).append(' ')
                  .append(issue.title())
                  .append("  ").append(issue.labels()).append('\n');
            }
        }

        String insight = brain.think(
            "Summarise the single most important action from this brief in one short line. "
                + prCount + " open PRs across " + repos.size() + " repositories.");
        sb.append("\nInsight: ").append(insight).append('\n');
        return sb.toString();
    }

    private static String shortSha(String sha) {
        if (sha == null) {
            return "0000000";
        }
        return sha.length() > 7 ? sha.substring(0, 7) : sha;
    }
}
