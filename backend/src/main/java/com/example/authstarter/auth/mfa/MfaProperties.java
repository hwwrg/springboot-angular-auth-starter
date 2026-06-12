package com.example.authstarter.auth.mfa;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable parameters for TOTP multi-factor authentication.
 *
 * @param issuer the label shown by authenticator apps (and embedded in the otpauth URI)
 * @param recoveryCodeCount number of single-use recovery codes generated on enrollment
 * @param verificationStepTolerance accepted number of 30s time steps before/after now,
 *        absorbing small clock drift between the server and the authenticator
 */
@ConfigurationProperties(prefix = "authstarter.auth.mfa")
public record MfaProperties(String issuer, int recoveryCodeCount, int verificationStepTolerance) {

    public MfaProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "Spring Boot Angular Auth Starter";
        }
        if (recoveryCodeCount <= 0) {
            recoveryCodeCount = 10;
        }
        if (verificationStepTolerance < 0) {
            verificationStepTolerance = 1;
        }
    }
}
