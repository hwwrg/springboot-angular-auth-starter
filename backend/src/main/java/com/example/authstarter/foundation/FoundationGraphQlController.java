package com.example.authstarter.foundation;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.security.AuthStarterRole;
import com.example.authstarter.security.RbacAuthorizationService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class FoundationGraphQlController {

    private final AuthenticatedPrincipalResolver principalResolver;
    private final CurrentUserContextService currentUserContextService;
    private final RbacAuthorizationService authorizationService;

    public FoundationGraphQlController(
            AuthenticatedPrincipalResolver principalResolver,
            CurrentUserContextService currentUserContextService,
            RbacAuthorizationService authorizationService) {
        this.principalResolver = principalResolver;
        this.currentUserContextService = currentUserContextService;
        this.authorizationService = authorizationService;
    }

    @QueryMapping
    public CurrentUserProfilePayload currentUserProfile() {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return currentUserContextService.findCurrentUserProfile(principal).orElse(null);
    }

    @QueryMapping
    public OrganizationContextPayload currentOrganizationContext() {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return currentUserContextService.findCurrentOrganizationContext(principal).orElse(null);
    }

    @QueryMapping
    public List<OrganizationSummaryPayload> foundationOrganizations() {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        Set<AuthStarterRole> resolvedRoles = authorizationService.requireAnyRole(
                AuthStarterRole.SUPERADMIN,
                AuthStarterRole.ORG_ADMIN);
        return currentUserContextService.listVisibleOrganizations(principal, resolvedRoles)
                .stream()
                .sorted(Comparator.comparing(OrganizationSummaryPayload::displayName))
                .toList();
    }
}
