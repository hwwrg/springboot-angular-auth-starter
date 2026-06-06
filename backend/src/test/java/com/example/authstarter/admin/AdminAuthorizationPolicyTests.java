package com.example.authstarter.admin;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.foundation.OrganizationContextPayload;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AdminAuthorizationPolicyTests {

    private static final UUID WORKSPACE_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID ORGANIZATION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID SUPERADMIN_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID ORG_ADMIN_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");

    private final AdminAuthorizationPolicy policy = new AdminAuthorizationPolicy();

    @Test
    void superadminCanAccessAdminManagement() {
        assertThatCode(() -> policy.authorizeAdminAccess(
                principal(SUPERADMIN_ID, "SUPERADMIN"),
                organization("USER")))
                .doesNotThrowAnyException();
    }

    @Test
    void currentOrganizationAdminCanAccessAdminManagement() {
        assertThatCode(() -> policy.authorizeAdminAccess(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN")))
                .doesNotThrowAnyException();
    }

    @Test
    void userCannotAccessAdminManagement() {
        assertThatThrownBy(() -> policy.authorizeAdminAccess(
                principal(USER_ID, "USER"),
                organization("USER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminInAnotherOrganizationCannotManageCurrentOrganization() {
        assertThatThrownBy(() -> policy.authorizeAdminAccess(
                principal(ORG_ADMIN_ID, List.of("ORG_ADMIN", "USER")),
                organization("USER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminCannotModifySuperadminUsers() {
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                SUPERADMIN_ID,
                user(SUPERADMIN_ID, "SUPERADMIN", "ACTIVE", "ACTIVE"),
                "SUPERADMIN",
                "ACTIVE",
                "ACTIVE",
                true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminCannotAssignSuperadminRole() {
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                USER_ID,
                user(USER_ID, "USER", "ACTIVE", "ACTIVE"),
                "SUPERADMIN",
                "ACTIVE",
                "ACTIVE",
                true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminCannotAssignOrganizationAdminRoleByDefault() {
        assertThatThrownBy(() -> policy.authorizeCreate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                "ORG_ADMIN"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                USER_ID,
                user(USER_ID, "USER", "ACTIVE", "ACTIVE"),
                "ORG_ADMIN",
                "ACTIVE",
                "ACTIVE",
                true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminCannotModifyOwnAdministrativeContext() {
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                ORG_ADMIN_ID,
                user(ORG_ADMIN_ID, "ORG_ADMIN", "ACTIVE", "ACTIVE"),
                "USER",
                "ACTIVE",
                "ACTIVE",
                true))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                ORG_ADMIN_ID,
                user(ORG_ADMIN_ID, "ORG_ADMIN", "ACTIVE", "ACTIVE"),
                "ORG_ADMIN",
                "SUSPENDED",
                "ACTIVE",
                true))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                ORG_ADMIN_ID,
                user(ORG_ADMIN_ID, "ORG_ADMIN", "ACTIVE", "ACTIVE"),
                "ORG_ADMIN",
                "ACTIVE",
                "SUSPENDED",
                true))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                ORG_ADMIN_ID,
                user(ORG_ADMIN_ID, "ORG_ADMIN", "ACTIVE", "ACTIVE", true),
                "ORG_ADMIN",
                "ACTIVE",
                "ACTIVE",
                false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizationAdminCanModifyOwnDisplayNameWhenAdministrativeContextIsUnchanged() {
        assertThatCode(() -> policy.authorizeUpdate(
                principal(ORG_ADMIN_ID, "ORG_ADMIN"),
                organization("ORG_ADMIN"),
                ORG_ADMIN_ID,
                user(ORG_ADMIN_ID, "ORG_ADMIN", "ACTIVE", "ACTIVE", true),
                "ORG_ADMIN",
                "ACTIVE",
                "ACTIVE",
                true))
                .doesNotThrowAnyException();
    }

    @Test
    void superadminCanAssignAndModifySuperadminUsers() {
        assertThatCode(() -> policy.authorizeCreate(
                principal(SUPERADMIN_ID, "SUPERADMIN"),
                organization("USER"),
                "SUPERADMIN"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.authorizeUpdate(
                principal(SUPERADMIN_ID, "SUPERADMIN"),
                organization("USER"),
                USER_ID,
                user(USER_ID, "USER", "ACTIVE", "ACTIVE"),
                "SUPERADMIN",
                "ACTIVE",
                "ACTIVE",
                true))
                .doesNotThrowAnyException();
    }

    @Test
    void superadminCanAssignOrganizationAdminUsers() {
        assertThatCode(() -> policy.authorizeCreate(
                principal(SUPERADMIN_ID, "SUPERADMIN"),
                organization("ORG_ADMIN"),
                "ORG_ADMIN"))
                .doesNotThrowAnyException();
    }

    private AuthPrincipal principal(UUID id, String role) {
        return principal(id, List.of(role));
    }

    private AuthPrincipal principal(UUID id, List<String> roles) {
        String primaryRole = roles.get(0);
        return new AuthPrincipal(
                id.toString(),
                primaryRole.toLowerCase().replace("_", "-") + "@example.test",
                primaryRole,
                roles);
    }

    private OrganizationContextPayload organization(String role) {
        return new OrganizationContextPayload(
                ORGANIZATION_ID.toString(),
                "Auth Starter Local",
                "ACTIVE",
                WORKSPACE_ID.toString(),
                "local-authstarter",
                "ACTIVE",
                role);
    }

    private AdminUserSummaryPayload user(UUID id, String role, String userStatus, String membershipStatus) {
        return user(id, role, userStatus, membershipStatus, true);
    }

    private AdminUserSummaryPayload user(
            UUID id,
            String role,
            String userStatus,
            String membershipStatus,
            boolean primaryMembership) {
        return new AdminUserSummaryPayload(
                id.toString(),
                role.toLowerCase().replace("_", "-") + "@example.test",
                role,
                userStatus,
                role,
                membershipStatus,
                primaryMembership,
                "2026-01-01T00:00:00Z",
                "2026-01-01T00:00:00Z");
    }
}
