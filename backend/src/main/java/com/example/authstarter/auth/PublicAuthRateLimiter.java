package com.example.authstarter.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class PublicAuthRateLimiter {

    // Starter-friendly single-process limiter. Production multi-instance deployments should use
    // distributed storage such as Redis so attempts are counted consistently across nodes.
    private static final int MAX_BUCKETS_BEFORE_CLEANUP = 10_000;
    private static final long MAX_WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public PublicAuthRateLimiter() {
        this(Clock.systemUTC());
    }

    PublicAuthRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void checkEmail(PublicAuthFlow flow, String email) {
        check(flow, normalizeEmail(email));
    }

    public void checkToken(PublicAuthFlow flow, String token) {
        String normalizedToken = StringUtils.trimWhitespace(token);
        check(flow, StringUtils.hasText(normalizedToken) ? SecurityTokenHasher.sha256(normalizedToken) : "blank-token");
    }

    private void check(PublicAuthFlow flow, String discriminator) {
        long nowMillis = clock.millis();
        String key = flow.name() + ":" + clientAddress() + ":" + discriminator;
        Bucket bucket = buckets.compute(key, (ignored, existing) -> nextBucket(flow, existing, nowMillis));
        if (bucket.count() > flow.maxAttempts()) {
            throw new PublicAuthRateLimitExceededException("Too many attempts. Please wait before trying again.");
        }
        cleanupExpiredBuckets(nowMillis);
    }

    private Bucket nextBucket(PublicAuthFlow flow, Bucket existing, long nowMillis) {
        if (existing == null || nowMillis - existing.windowStartMillis() >= flow.window().toMillis()) {
            return new Bucket(nowMillis, 1);
        }
        return new Bucket(existing.windowStartMillis(), existing.count() + 1);
    }

    private String clientAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            if (StringUtils.hasText(request.getRemoteAddr())) {
                return request.getRemoteAddr();
            }
        }
        return "unknown-client";
    }

    private String normalizeEmail(String email) {
        String trimmed = StringUtils.trimWhitespace(email);
        String normalized = trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
        return StringUtils.hasText(normalized) ? normalized : "blank-email";
    }

    private void cleanupExpiredBuckets(long nowMillis) {
        if (buckets.size() <= MAX_BUCKETS_BEFORE_CLEANUP) {
            return;
        }
        buckets.entrySet().removeIf(entry ->
                nowMillis - entry.getValue().windowStartMillis() >= MAX_WINDOW_MILLIS);
    }

    private record Bucket(long windowStartMillis, int count) {
    }

    public enum PublicAuthFlow {
        LOGIN(10, Duration.ofMinutes(1)),
        REQUEST_PASSWORD_RESET(5, Duration.ofMinutes(15)),
        RESET_PASSWORD(10, Duration.ofMinutes(15)),
        ACCEPT_USER_INVITE(10, Duration.ofMinutes(15));

        private final int maxAttempts;
        private final Duration window;

        PublicAuthFlow(int maxAttempts, Duration window) {
            this.maxAttempts = maxAttempts;
            this.window = window;
        }

        int maxAttempts() {
            return maxAttempts;
        }

        Duration window() {
            return window;
        }
    }
}
