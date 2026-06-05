package com.example.authstarter.admin;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.authstarter.auth.AuthPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AdminAuthorizationPolicyTests {

    private static final UUID SUPERADMIN_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID ORG_ADMIN_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");

    private final AdminAuthorizationPolicy policy = new AdminAuthorizationPolicy();

    @Test
    void organizationAdminCannotModifySuperadminUsers() {
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                SUPERADMIN_ID,
                user(SUPERADMIN_ID, "SUPERADMIN", "ACTIVE", "ACTIVE"),
                "SUPERADMIN",
                "ACTIVE",
                "ACTIVE"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminCannotAssignSuperadminRole() {
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                USER_ID,
                user(USER_ID, "USER", "ACTIVE", "ACTIVE"),
                "SUPERADMIN",
                "ACTIVE",
                "ACTIVE"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminCannotAssignOrganizationAdminRoleByDefault() {
        assertThatThrownBy(() -> policy.authorizeCreate(principal(ORG_ADMIN_ID, "ORG_ADMIN"), "ORG_ADMIN"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                USER_ID,
                user(USER_ID, "USER", "ACTIVE", "ACTIVE"),
                "ORG_ADMIN",
                "ACTIVE",
                "ACTIVE"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminCannotModifyOwnRoleOrStatus() {
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                ORG_ADMIN_ID,
                user(ORG_ADMIN_ID, "ORG_ADMIN", "ACTIVE", "ACTIVE"),
                "USER",
                "ACTIVE",
                "ACTIVE"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                ORG_ADMIN_ID,
                user(ORG_ADMIN_ID, "ORG_ADMIN", "ACTIVE", "ACTIVE"),
                "ORG_ADMIN",
                "SUSPENDED",
                "ACTIVE"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                ORG_ADMIN_ID,
                user(ORG_ADMIN_ID, "ORG_ADMIN", "ACTIVE", "ACTIVE"),
                "ORG_ADMIN",
                "ACTIVE",
                "SUSPENDED"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void superadminCanAssignAndModifySuperadminUsers() {
        assertThatCode(() -> policy.authorizeCreate(principal(SUPERADMIN_ID, "SUPERADMIN"), "SUPERADMIN"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.authorizeUpdate(
                principal(SUPERADMIN_ID, "SUPERADMIN"),
                USER_ID,
                user(USER_ID, "USER", "ACTIVE", "ACTIVE"),
                "SUPERADMIN",
                "ACTIVE",
                "ACTIVE"))
                .doesNotThrowAnyException();
    }

    private AuthPrincipal principal(UUID id, String role) {
        return new AuthPrincipal(id.toString(), role.toLowerCase() + "@example.test", role, List.of(role));
    }

    private AdminUserSummaryPayload user(UUID id, String role, String userStatus, String membershipStatus) {
        return new AdminUserSummaryPayload(
                id.toString(),
                role.toLowerCase() + "@example.test",
                role,
                userStatus,
                role,
                membershipStatus,
                true,
                "2026-01-01T00:00:00Z",
                "2026-01-01T00:00:00Z");
    }
}
