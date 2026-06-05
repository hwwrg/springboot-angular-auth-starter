package com.example.authstarter.admin;

import com.example.authstarter.auth.AuthPrincipal;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;

final class AdminAuthorizationPolicy {

    static final boolean ORG_ADMIN_CAN_ASSIGN_ORG_ADMIN = false;

    void authorizeCreate(AuthPrincipal principal, String role) {
        if ("SUPERADMIN".equals(role) && !isSuperAdmin(principal)) {
            throw new AccessDeniedException("Only a SUPERADMIN can assign the SUPERADMIN role.");
        }
        if ("ORG_ADMIN".equals(role) && isOrgAdmin(principal) && !ORG_ADMIN_CAN_ASSIGN_ORG_ADMIN) {
            throw new AccessDeniedException("ORG_ADMIN cannot assign the ORG_ADMIN role.");
        }
    }

    void authorizeUpdate(
            AuthPrincipal principal,
            UUID userId,
            AdminUserSummaryPayload existing,
            String role,
            String userStatus,
            String membershipStatus) {
        boolean actorIsSuperAdmin = isSuperAdmin(principal);
        if (("SUPERADMIN".equals(existing.role()) || "SUPERADMIN".equals(role)) && !actorIsSuperAdmin) {
            throw new AccessDeniedException("Only a SUPERADMIN can assign or modify SUPERADMIN users.");
        }
        if (isOrgAdmin(principal)
                && "ORG_ADMIN".equals(role)
                && !"ORG_ADMIN".equals(existing.role())
                && !ORG_ADMIN_CAN_ASSIGN_ORG_ADMIN) {
            throw new AccessDeniedException("ORG_ADMIN cannot assign the ORG_ADMIN role.");
        }
        if (isOrgAdmin(principal)
                && userId.equals(principalUserId(principal))
                && roleOrStatusChanged(existing, role, userStatus, membershipStatus)) {
            throw new AccessDeniedException("ORG_ADMIN cannot modify their own role or status.");
        }
    }

    private boolean roleOrStatusChanged(
            AdminUserSummaryPayload existing,
            String role,
            String userStatus,
            String membershipStatus) {
        return !existing.role().equals(role)
                || !existing.status().equals(userStatus)
                || !existing.membershipStatus().equals(membershipStatus);
    }

    private boolean isSuperAdmin(AuthPrincipal principal) {
        return principal != null && principal.roles().contains("SUPERADMIN");
    }

    private boolean isOrgAdmin(AuthPrincipal principal) {
        return principal != null && principal.roles().contains("ORG_ADMIN") && !isSuperAdmin(principal);
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
