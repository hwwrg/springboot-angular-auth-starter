package com.example.authstarter.auth;

public record AuthSessionPayload(boolean authenticated, AuthPrincipal principal, boolean mustChangePassword) {

    public static AuthSessionPayload anonymous() {
        return new AuthSessionPayload(false, null, false);
    }

    public static AuthSessionPayload authenticated(AuthPrincipal principal) {
        return new AuthSessionPayload(true, principal, principal.mustChangePassword());
    }
}
