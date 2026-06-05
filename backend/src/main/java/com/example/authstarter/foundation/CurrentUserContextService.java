package com.example.authstarter.foundation;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.security.AuthStarterRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CurrentUserContextService {

    UserSessionIdentity resolveSessionIdentity(String email, String fallbackDisplayName, AuthStarterRole fallbackRole);

    default boolean supportsActiveSessionValidation() {
        return false;
    }

    Optional<CurrentUserProfilePayload> findCurrentUserProfile(AuthPrincipal principal);

    Optional<OrganizationContextPayload> findCurrentOrganizationContext(AuthPrincipal principal);

    List<OrganizationSummaryPayload> listVisibleOrganizations(AuthPrincipal principal, Set<AuthStarterRole> resolvedRoles);
}
