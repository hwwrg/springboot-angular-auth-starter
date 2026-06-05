package com.example.authstarter.auth;

import com.example.authstarter.foundation.OrganizationMembershipPayload;
import com.example.authstarter.foundation.CurrentUserContextService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedSessionValidationService {

    private static final String BASELINE_OPERATOR_ID = "baseline-operator";

    private final CurrentUserContextService currentUserContextService;

    public AuthenticatedSessionValidationService(CurrentUserContextService currentUserContextService) {
        this.currentUserContextService = currentUserContextService;
    }

    public Optional<Authentication> refresh(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return Optional.ofNullable(authentication);
        }

        if (BASELINE_OPERATOR_ID.equals(principal.id())) {
            return Optional.of(authentication);
        }

        if (!currentUserContextService.supportsActiveSessionValidation()) {
            return Optional.of(authentication);
        }

        return currentUserContextService.findCurrentUserProfile(principal)
                .filter(profile -> profile.id().equals(principal.id()))
                .flatMap(profile -> {
                    List<String> activeRoles = activeRoleNames(profile.memberships());
                    if (activeRoles.isEmpty()) {
                        return Optional.empty();
                    }

                    AuthPrincipal refreshedPrincipal = new AuthPrincipal(
                            profile.id(),
                            profile.email(),
                            profile.displayName(),
                            activeRoles,
                            principal.mustChangePassword());
                    UsernamePasswordAuthenticationToken refreshedAuthentication =
                            new UsernamePasswordAuthenticationToken(
                                    refreshedPrincipal,
                                    null,
                                    activeRoles.stream()
                                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                            .toList());
                    refreshedAuthentication.setDetails(authentication.getDetails());
                    return Optional.of(refreshedAuthentication);
                });
    }

    private List<String> activeRoleNames(List<OrganizationMembershipPayload> memberships) {
        Set<String> roles = new LinkedHashSet<>();
        memberships.stream()
                .filter(membership -> "ACTIVE".equals(membership.status()))
                .map(OrganizationMembershipPayload::role)
                .forEach(roles::add);
        return List.copyOf(roles);
    }
}
