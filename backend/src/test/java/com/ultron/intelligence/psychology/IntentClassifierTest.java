package com.ultron.intelligence.psychology;

import static org.assertj.core.api.Assertions.assertThat;

import com.ultron.intelligence.psychology.IntentClassifier.Intent;
import org.junit.jupiter.api.Test;

/** Pure unit test for the offline intent classifier. */
class IntentClassifierTest {

    private final IntentClassifier classifier = new IntentClassifier();

    @Test
    void classifiesCoreIntents() {
        assertThat(classifier.classify("remember to renew the domain")).isEqualTo(Intent.CAPTURE);
        assertThat(classifier.classify("plan my day around the release")).isEqualTo(Intent.PLAN);
        assertThat(classifier.classify("show me my watchlist")).isEqualTo(Intent.TRADE_WATCH);
        assertThat(classifier.classify("buy 10 shares of AAPL")).isEqualTo(Intent.TRADE_LIVE);
        assertThat(classifier.classify("what is my aws region")).isEqualTo(Intent.QUESTION);
        assertThat(classifier.classify("hello")).isEqualTo(Intent.SMALL_TALK);
    }

    @Test
    void liveTradeTakesPrecedenceOverWatch() {
        assertThat(classifier.classify("sell my market position now")).isEqualTo(Intent.TRADE_LIVE);
    }

    @Test
    void blankDefaultsToQuestion() {
        assertThat(classifier.classify("  ")).isEqualTo(Intent.QUESTION);
    }
}
