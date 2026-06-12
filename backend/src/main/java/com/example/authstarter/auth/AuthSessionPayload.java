package com.example.authstarter.auth;

public record AuthSessionPayload(
        boolean authenticated, AuthPrincipal principal, boolean mustChangePassword, boolean mfaRequired) {

    public static AuthSessionPayload anonymous() {
        return new AuthSessionPayload(false, null, false, false);
    }

    public static AuthSessionPayload authenticated(AuthPrincipal principal) {
        return new AuthSessionPayload(true, principal, principal.mustChangePassword(), false);
    }

    /**
     * First-factor credentials were accepted but a second factor is still
     * required: the caller is not authenticated until {@code verifyMfa} succeeds.
     */
    public static AuthSessionPayload mfaChallenge() {
        return new AuthSessionPayload(false, null, false, true);
    }
}
