package com.example.authstarter.auth;

import com.example.authstarter.security.AuthStarterRole;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedRoleResolver {

    public Set<AuthStarterRole> resolveCurrentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return Set.of();
        }

        EnumSet<AuthStarterRole> roles = EnumSet.noneOf(AuthStarterRole.class);
        principal.roles().stream()
                .map(AuthStarterRole::fromSessionValue)
                .forEach(roles::add);
        return Set.copyOf(roles);
    }
}
