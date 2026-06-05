package com.example.authstarter.foundation;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.security.AuthStarterRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
class NoopCurrentUserContextService implements CurrentUserContextService {

    @Override
    public UserSessionIdentity resolveSessionIdentity(String email, String fallbackDisplayName, AuthStarterRole fallbackRole) {
        return new UserSessionIdentity("baseline-operator", email, fallbackDisplayName, List.of(fallbackRole.name()));
    }

    @Override
    public Optional<CurrentUserProfilePayload> findCurrentUserProfile(AuthPrincipal principal) {
        return Optional.empty();
    }

    @Override
    public Optional<OrganizationContextPayload> findCurrentOrganizationContext(AuthPrincipal principal) {
        return Optional.empty();
    }

    @Override
    public List<OrganizationSummaryPayload> listVisibleOrganizations(AuthPrincipal principal, Set<AuthStarterRole> resolvedRoles) {
        return List.of();
    }
}
