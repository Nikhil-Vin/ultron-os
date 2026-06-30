package com.ultron.intelligence.psychology;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.intelligence.psychology.IntentClassifier.Intent;
import org.junit.jupiter.api.Test;

/** Pure unit tests for the priority scorer and the online feedback loop. */
class PsychologyEngineTest {

    private final PriorityScorer scorer = new PriorityScorer();

    @Test
    void urgentTextScoresHigherThanLowPriority() {
        double urgent = scorer.score("URGENT: production is down, fix immediately");
        double low = scorer.score("someday maybe refactor the logging, nice to have");
        assertThat(urgent).isGreaterThan(low);
        assertThat(urgent).isBetween(0.0, 1.0);
        assertThat(low).isBetween(0.0, 1.0);
    }

    @Test
    void blankScoresZero() {
        assertThat(scorer.score("  ")).isEqualTo(0.0);
    }

    @Test
    void feedbackReinforcesAndPenalises() {
        FeedbackLoop loop = new FeedbackLoop();
        double start = loop.weight(Intent.QUESTION);

        loop.record(Intent.QUESTION, true);
        loop.record(Intent.QUESTION, true);
        assertThat(loop.weight(Intent.QUESTION)).isGreaterThan(start);

        FeedbackLoop loop2 = new FeedbackLoop();
        loop2.record(Intent.PLAN, false);
        loop2.record(Intent.PLAN, false);
        assertThat(loop2.weight(Intent.PLAN)).isLessThan(start);
    }
}
