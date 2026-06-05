package com.example.authstarter.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PublicAuthRateLimiterTests {

    @Test
    void limitsRepeatedLoginAttemptsForSameEmailAndClientWindow() {
        MutableClock clock = new MutableClock();
        PublicAuthRateLimiter rateLimiter = new PublicAuthRateLimiter(clock);

        for (int i = 0; i < 10; i++) {
            rateLimiter.checkEmail(PublicAuthRateLimiter.PublicAuthFlow.LOGIN, "Operator@Example.Test");
        }

        assertThatThrownBy(() ->
                rateLimiter.checkEmail(PublicAuthRateLimiter.PublicAuthFlow.LOGIN, "operator@example.test"))
                .isInstanceOf(PublicAuthRateLimitExceededException.class);
    }

    @Test
    void resetsAttemptsAfterWindowExpires() {
        MutableClock clock = new MutableClock();
        PublicAuthRateLimiter rateLimiter = new PublicAuthRateLimiter(clock);

        for (int i = 0; i < 10; i++) {
            rateLimiter.checkEmail(PublicAuthRateLimiter.PublicAuthFlow.LOGIN, "operator@example.test");
        }

        clock.advance(Duration.ofMinutes(1));

        assertThatCode(() ->
                rateLimiter.checkEmail(PublicAuthRateLimiter.PublicAuthFlow.LOGIN, "operator@example.test"))
                .doesNotThrowAnyException();
    }

    @Test
    void tokenFlowsHashTokenDiscriminatorsBeforeCounting() {
        MutableClock clock = new MutableClock();
        PublicAuthRateLimiter rateLimiter = new PublicAuthRateLimiter(clock);

        for (int i = 0; i < 10; i++) {
            rateLimiter.checkToken(PublicAuthRateLimiter.PublicAuthFlow.RESET_PASSWORD, "token-value");
        }

        assertThatThrownBy(() ->
                rateLimiter.checkToken(PublicAuthRateLimiter.PublicAuthFlow.RESET_PASSWORD, " token-value "))
                .isInstanceOf(PublicAuthRateLimitExceededException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
