package com.example.authstarter.foundation;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.security.AuthStarterRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Primary
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
class JdbcCurrentUserContextService implements CurrentUserContextService {

    private final JdbcClient jdbcClient;

    JdbcCurrentUserContextService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean supportsActiveSessionValidation() {
        return true;
    }

    @Override
    public UserSessionIdentity resolveSessionIdentity(String email, String fallbackDisplayName, AuthStarterRole fallbackRole) {
        return findCurrentUserProfileByEmail(email)
                .map(profile -> new UserSessionIdentity(
                        profile.id(),
                        profile.email(),
                        profile.displayName(),
                        activeRoleNames(profile.memberships())))
                .orElseGet(() -> {
                    if (userExistsByEmail(email)) {
                        throw new BadCredentialsException("Invalid email or password.");
                    }
                    return new UserSessionIdentity(
                            "baseline-operator",
                            normalizeEmail(email),
                            fallbackDisplayName,
                            List.of(fallbackRole.name()));
                });
    }

    @Override
    public Optional<CurrentUserProfilePayload> findCurrentUserProfile(AuthPrincipal principal) {
        return findCurrentUserProfileByEmail(principal.email());
    }

    @Override
    public Optional<OrganizationContextPayload> findCurrentOrganizationContext(AuthPrincipal principal) {
        return findCurrentUserProfile(principal).map(CurrentUserProfilePayload::currentOrganization);
    }

    @Override
    public List<OrganizationSummaryPayload> listVisibleOrganizations(AuthPrincipal principal, Set<AuthStarterRole> resolvedRoles) {
        if (resolvedRoles.contains(AuthStarterRole.SUPERADMIN)) {
            return jdbcClient.sql("""
                            select c.id,
                                   c.display_name,
                                   c.legal_name,
                                   c.status,
                                   tb.id as workspace_id,
                                   tb.code as workspace_code
                              from organizations c
                              join workspaces tb on tb.id = c.workspace_id
                             order by c.display_name
                            """)
                    .query(this::mapOrganizationSummary)
                    .list();
        }

        return jdbcClient.sql("""
                        select c.id,
                               c.display_name,
                               c.legal_name,
                               c.status,
                               tb.id as workspace_id,
                               tb.code as workspace_code
                          from app_users u
                          join organization_memberships cm on cm.user_id = u.id
                          join organizations c on c.id = cm.organization_id
                          join workspaces tb on tb.id = c.workspace_id
                         where u.email = :email
                           and u.status = 'ACTIVE'
                           and cm.status = 'ACTIVE'
                           and c.status = 'ACTIVE'
                           and tb.status = 'ACTIVE'
                         order by c.display_name
                        """)
                .param("email", normalizeEmail(principal.email()))
                .query(this::mapOrganizationSummary)
                .list();
    }

    private Optional<CurrentUserProfilePayload> findCurrentUserProfileByEmail(String email) {
        return jdbcClient.sql("""
                        select u.id as user_id,
                               u.email,
                               u.display_name as user_display_name,
                               u.status as user_status,
                               c.id as organization_id,
                               c.display_name as organization_display_name,
                               c.status as organization_status,
                               tb.id as workspace_id,
                               tb.code as workspace_code,
                               tb.status as workspace_status,
                               cm.role,
                               cm.status as membership_status,
                               cm.primary_membership
                          from app_users u
                          join organization_memberships cm on cm.user_id = u.id
                          join organizations c on c.id = cm.organization_id
                          join workspaces tb on tb.id = c.workspace_id
                         where u.email = :email
                           and u.status = 'ACTIVE'
                           and cm.status = 'ACTIVE'
                           and c.status = 'ACTIVE'
                           and tb.status = 'ACTIVE'
                         order by cm.primary_membership desc, c.display_name
                        """)
                .param("email", normalizeEmail(email))
                .query(this::mapMembershipRow)
                .list()
                .stream()
                .collect(ProfileAccumulator::new, ProfileAccumulator::add, ProfileAccumulator::combine)
                .toPayload();
    }

    private boolean userExistsByEmail(String email) {
        return jdbcClient.sql("""
                        select exists (
                            select 1
                              from app_users
                             where email = :email
                        )
                        """)
                .param("email", normalizeEmail(email))
                .query(Boolean.class)
                .single();
    }

    private ProfileMembershipRow mapMembershipRow(ResultSet rs, int rowNumber) throws SQLException {
        return new ProfileMembershipRow(
                rs.getString("user_id"),
                rs.getString("email"),
                rs.getString("user_display_name"),
                rs.getString("user_status"),
                rs.getString("organization_id"),
                rs.getString("organization_display_name"),
                rs.getString("organization_status"),
                rs.getString("workspace_id"),
                rs.getString("workspace_code"),
                rs.getString("workspace_status"),
                rs.getString("role"),
                rs.getString("membership_status"),
                rs.getBoolean("primary_membership"));
    }

    private OrganizationSummaryPayload mapOrganizationSummary(ResultSet rs, int rowNumber) throws SQLException {
        return new OrganizationSummaryPayload(
                rs.getString("id"),
                rs.getString("display_name"),
                rs.getString("legal_name"),
                rs.getString("status"),
                rs.getString("workspace_id"),
                rs.getString("workspace_code"));
    }

    private List<String> activeRoleNames(List<OrganizationMembershipPayload> memberships) {
        Set<String> roles = new LinkedHashSet<>();
        memberships.stream()
                .filter(membership -> "ACTIVE".equals(membership.status()))
                .map(OrganizationMembershipPayload::role)
                .forEach(roles::add);

        return List.copyOf(roles);
    }

    private String normalizeEmail(String email) {
        return StringUtils.trimWhitespace(email).toLowerCase(Locale.ROOT);
    }

    private record ProfileMembershipRow(
            String userId,
            String email,
            String userDisplayName,
            String userStatus,
            String organizationId,
            String organizationDisplayName,
            String organizationStatus,
            String workspaceId,
            String workspaceCode,
            String workspaceStatus,
            String role,
            String membershipStatus,
            boolean primaryMembership) {
    }

    private static final class ProfileAccumulator {
        private String userId;
        private String email;
        private String displayName;
        private String status;
        private OrganizationContextPayload currentOrganization;
        private final List<OrganizationMembershipPayload> memberships = new java.util.ArrayList<>();

        private void add(ProfileMembershipRow row) {
            this.userId = row.userId();
            this.email = row.email();
            this.displayName = row.userDisplayName();
            this.status = row.userStatus();

            if (row.organizationId() == null) {
                return;
            }

            OrganizationMembershipPayload membership = new OrganizationMembershipPayload(
                    row.organizationId(),
                    row.organizationDisplayName(),
                    row.organizationStatus(),
                    row.workspaceId(),
                    row.workspaceCode(),
                    row.role(),
                    row.membershipStatus(),
                    row.primaryMembership());
            memberships.add(membership);

            if (currentOrganization == null && row.primaryMembership()) {
                currentOrganization = new OrganizationContextPayload(
                        row.organizationId(),
                        row.organizationDisplayName(),
                        row.organizationStatus(),
                        row.workspaceId(),
                        row.workspaceCode(),
                        row.workspaceStatus(),
                        row.role());
            }
        }

        private void combine(ProfileAccumulator other) {
            if (userId == null) {
                userId = other.userId;
                email = other.email;
                displayName = other.displayName;
                status = other.status;
            }
            if (currentOrganization == null) {
                currentOrganization = other.currentOrganization;
            }
            memberships.addAll(other.memberships);
        }

        private Optional<CurrentUserProfilePayload> toPayload() {
            if (userId == null) {
                return Optional.empty();
            }

            OrganizationContextPayload resolvedCurrentOrganization = currentOrganization;
            if (resolvedCurrentOrganization == null && !memberships.isEmpty()) {
                OrganizationMembershipPayload firstMembership = memberships.getFirst();
                resolvedCurrentOrganization = new OrganizationContextPayload(
                        firstMembership.organizationId(),
                        firstMembership.organizationDisplayName(),
                        firstMembership.organizationStatus(),
                        firstMembership.workspaceId(),
                        firstMembership.workspaceCode(),
                        "ACTIVE",
                        firstMembership.role());
            }

            return Optional.of(new CurrentUserProfilePayload(
                    userId,
                    email,
                    displayName,
                    status,
                    resolvedCurrentOrganization,
                    List.copyOf(memberships)));
        }
    }
}
