package com.example.authstarter.admin;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.foundation.OrganizationContextPayload;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;

final class AdminAuthorizationPolicy {

    static final boolean ORG_ADMIN_CAN_ASSIGN_ORG_ADMIN = false;

    void authorizeAdminAccess(AuthPrincipal principal, OrganizationContextPayload currentOrganization) {
        if (isSuperAdmin(principal, currentOrganization) || isOrgAdmin(principal, currentOrganization)) {
            return;
        }

        throw new AccessDeniedException("Admin management requires SUPERADMIN or current organization ORG_ADMIN.");
    }

    void authorizeCreate(AuthPrincipal principal, OrganizationContextPayload currentOrganization, String role) {
        if ("SUPERADMIN".equals(role) && !isSuperAdmin(principal, currentOrganization)) {
            throw new AccessDeniedException("Only a SUPERADMIN can assign the SUPERADMIN role.");
        }
        if ("ORG_ADMIN".equals(role)
                && isOrganizationAdminOnly(principal, currentOrganization)
                && !ORG_ADMIN_CAN_ASSIGN_ORG_ADMIN) {
            throw new AccessDeniedException("ORG_ADMIN cannot assign the ORG_ADMIN role.");
        }
    }

    void authorizeUpdate(
            AuthPrincipal principal,
            OrganizationContextPayload currentOrganization,
            UUID userId,
            AdminUserSummaryPayload existing,
            String role,
            String userStatus,
            String membershipStatus,
            boolean primaryMembership) {
        boolean actorIsSuperAdmin = isSuperAdmin(principal, currentOrganization);
        if (("SUPERADMIN".equals(existing.role()) || "SUPERADMIN".equals(role)) && !actorIsSuperAdmin) {
            throw new AccessDeniedException("Only a SUPERADMIN can assign or modify SUPERADMIN users.");
        }
        if (isOrganizationAdminOnly(principal, currentOrganization)
                && "ORG_ADMIN".equals(role)
                && !"ORG_ADMIN".equals(existing.role())
                && !ORG_ADMIN_CAN_ASSIGN_ORG_ADMIN) {
            throw new AccessDeniedException("ORG_ADMIN cannot assign the ORG_ADMIN role.");
        }
        if (isOrganizationAdminOnly(principal, currentOrganization)
                && userId.equals(principalUserId(principal))
                && administrativeContextChanged(existing, role, userStatus, membershipStatus, primaryMembership)) {
            throw new AccessDeniedException("ORG_ADMIN cannot modify their own administrative context.");
        }
    }

    private boolean administrativeContextChanged(
            AdminUserSummaryPayload existing,
            String role,
            String userStatus,
            String membershipStatus,
            boolean primaryMembership) {
        return !existing.role().equals(role)
                || !existing.status().equals(userStatus)
                || !existing.membershipStatus().equals(membershipStatus)
                || existing.primaryMembership() != primaryMembership;
    }

    private boolean isSuperAdmin(AuthPrincipal principal, OrganizationContextPayload currentOrganization) {
        return principal != null
                && (principal.roles().contains("SUPERADMIN")
                        || (currentOrganization != null && "SUPERADMIN".equals(currentOrganization.role())));
    }

    private boolean isOrgAdmin(AuthPrincipal principal, OrganizationContextPayload currentOrganization) {
        return principal != null && currentOrganization != null && "ORG_ADMIN".equals(currentOrganization.role());
    }

    private boolean isOrganizationAdminOnly(AuthPrincipal principal, OrganizationContextPayload currentOrganization) {
        return isOrgAdmin(principal, currentOrganization) && !isSuperAdmin(principal, currentOrganization);
    }

    private UUID principalUserId(AuthPrincipal principal) {
        if (principal == null || !StringUtils.hasText(principal.id())) {
            return null;
        }
        try {
            return UUID.fromString(principal.id());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
