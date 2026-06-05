package com.example.authstarter.foundation;

import com.example.authstarter.auth.AuthPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedPrincipalResolver {

    public AuthPrincipal requireCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new IllegalStateException("Authenticated principal is required.");
        }

        if (principal.mustChangePassword()) {
            throw new IllegalStateException("Password change is required before accessing protected app functionality.");
        }

        return principal;
    }
}
