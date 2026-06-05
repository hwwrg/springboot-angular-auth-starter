package com.example.authstarter.admin;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.auth.InvitationPasswordSetupUrlBuilder;
import com.example.authstarter.auth.UserInvitationService;
import com.example.authstarter.auth.UserInvitationTokenPayload;
import com.example.authstarter.foundation.OrganizationContextPayload;
import com.example.authstarter.foundation.CurrentUserContextService;
import com.example.authstarter.notification.NotificationEventPayload;
import com.example.authstarter.notification.NotificationService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

public interface AdminManagementService {

    AdminManagementBaselinePayload adminManagementBaseline(AuthPrincipal principal);

    AdminUserSummaryPayload createUser(AuthPrincipal principal, CreateAdminUserInput input);

    AdminUserSummaryPayload updateUser(AuthPrincipal principal, UpdateAdminUserInput input);
}

@Service
@ConditionalOnExpression("environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
class NoopAdminManagementService implements AdminManagementService {

    @Override
    public AdminManagementBaselinePayload adminManagementBaseline(AuthPrincipal principal) {
        throw new IllegalStateException("Admin management requires auth persistence.");
    }

    @Override
    public AdminUserSummaryPayload createUser(AuthPrincipal principal, CreateAdminUserInput input) {
        throw new IllegalStateException("Admin management requires auth persistence.");
    }

    @Override
    public AdminUserSummaryPayload updateUser(AuthPrincipal principal, UpdateAdminUserInput input) {
        throw new IllegalStateException("Admin management requires auth persistence.");
    }
}

@Service
@Primary
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
class JdbcAdminManagementService implements AdminManagementService {

    private static final Set<String> USER_CREATE_STATUSES = Set.of("INVITED", "ACTIVE");
    private static final Set<String> USER_STATUSES = Set.of("INVITED", "ACTIVE", "SUSPENDED", "ARCHIVED");
    private static final Set<String> MEMBERSHIP_CREATE_STATUSES = Set.of("INVITED", "ACTIVE");
    private static final Set<String> MEMBERSHIP_STATUSES = Set.of("ACTIVE", "INVITED", "SUSPENDED", "ARCHIVED");
    private static final Set<String> USER_ROLES = Set.of("SUPERADMIN", "ORG_ADMIN", "USER");

    private final CurrentUserContextService currentUserContextService;
    private final JdbcClient jdbcClient;
    private final NotificationService notificationService;
    private final UserInvitationService userInvitationService;
    private final InvitationPasswordSetupUrlBuilder invitationPasswordSetupUrlBuilder;

    JdbcAdminManagementService(
            CurrentUserContextService currentUserContextService,
            JdbcClient jdbcClient,
            NotificationService notificationService,
            UserInvitationService userInvitationService,
            InvitationPasswordSetupUrlBuilder invitationPasswordSetupUrlBuilder) {
        this.currentUserContextService = currentUserContextService;
        this.jdbcClient = jdbcClient;
        this.notificationService = notificationService;
        this.userInvitationService = userInvitationService;
        this.invitationPasswordSetupUrlBuilder = invitationPasswordSetupUrlBuilder;
    }

    @Override
    public AdminManagementBaselinePayload adminManagementBaseline(AuthPrincipal principal) {
        OrganizationContextPayload organization = requireCurrentOrganization(principal);
        List<AdminUserSummaryPayload> users = listUsers(organization);
        List<NotificationEventPayload> notificationEvents = notificationService.listNotificationEvents(principal);
        return new AdminManagementBaselinePayload(
                organization,
                users,
                notificationEvents,
                new AdminManagementTotalsPayload(users.size(), notificationEvents.size()));
    }

    @Override
    @Transactional
    public AdminUserSummaryPayload createUser(AuthPrincipal principal, CreateAdminUserInput input) {
        OrganizationContextPayload organization = requireCurrentOrganization(principal);
        ValidatedUserInput validated = validateCreateInput(principal, input);
        if (findUserIdByEmail(validated.email()) != null) {
            throw new IllegalArgumentException("User email already exists.");
        }

        UUID userId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into app_users (id, email, display_name, status)
                        values (:id, :email, :displayName, :status)
                        """)
                .param("id", userId)
                .param("email", validated.email())
                .param("displayName", validated.displayName())
                .param("status", validated.userStatus())
                .update();

        jdbcClient.sql("""
                        insert into organization_memberships (
                            id,
                            organization_id,
                            user_id,
                            role,
                            status,
                            primary_membership
                        ) values (
                            :id,
                            :organizationId,
                            :userId,
                            :role,
                            :status,
                            :primaryMembership
                        )
                        """)
                .param("id", UUID.randomUUID())
                .param("organizationId", organizationId(organization))
                .param("userId", userId)
                .param("role", validated.role())
                .param("status", validated.membershipStatus())
                .param("primaryMembership", validated.primaryMembership())
                .update();

        AdminUserSummaryPayload user = requireUser(organization, userId);
        UserInvitationTokenPayload invitation = userInvitationService.createInvitation(principal, userId);
        notificationService.sendUserInvitation(
                principal,
                organization,
                userId,
                user.displayName(),
                user.email(),
                invitationPasswordSetupUrlBuilder.setupUrl(invitation.rawToken()));
        return user;
    }

    @Override
    @Transactional
    public AdminUserSummaryPayload updateUser(AuthPrincipal principal, UpdateAdminUserInput input) {
        OrganizationContextPayload organization = requireCurrentOrganization(principal);
        UUID userId = parseRequiredUuid(input == null ? null : input.id(), "User id");
        ValidatedUserUpdateInput validated = validateUpdateInput(principal, input);
        requireUser(organization, userId);

        int updatedUserRows = jdbcClient.sql("""
                        update app_users u
                           set display_name = :displayName,
                               status = :userStatus,
                               updated_at = now()
                         where u.id = :userId
                           and exists (
                               select 1
                                 from organization_memberships cm
                                 join organizations c on c.id = cm.organization_id
                                where cm.user_id = u.id
                                  and cm.organization_id = :organizationId
                                  and c.workspace_id = :workspaceId
                           )
                        """)
                .param("userId", userId)
                .param("organizationId", organizationId(organization))
                .param("workspaceId", workspaceId(organization))
                .param("displayName", validated.displayName())
                .param("userStatus", validated.userStatus())
                .update();
        if (updatedUserRows != 1) {
            throw new IllegalArgumentException("User must belong to the active current organization context.");
        }

        if (validated.primaryMembership()) {
            jdbcClient.sql("""
                            update organization_memberships
                               set primary_membership = false,
                                   updated_at = now()
                             where user_id = :userId
                               and organization_id <> :organizationId
                            """)
                    .param("userId", userId)
                    .param("organizationId", organizationId(organization))
                    .update();
        }

        int updatedMembershipRows = jdbcClient.sql("""
                        update organization_memberships cm
                           set role = :role,
                               status = :membershipStatus,
                               primary_membership = :primaryMembership,
                               updated_at = now()
                         where cm.user_id = :userId
                           and cm.organization_id = :organizationId
                           and exists (
                               select 1
                                 from organizations c
                                where c.id = cm.organization_id
                                  and c.workspace_id = :workspaceId
                           )
                        """)
                .param("userId", userId)
                .param("organizationId", organizationId(organization))
                .param("workspaceId", workspaceId(organization))
                .param("role", validated.role())
                .param("membershipStatus", validated.membershipStatus())
                .param("primaryMembership", validated.primaryMembership())
                .update();
        if (updatedMembershipRows != 1) {
            throw new IllegalArgumentException("User membership must belong to the active current organization context.");
        }

        return requireUser(organization, userId);
    }

    private List<AdminUserSummaryPayload> listUsers(OrganizationContextPayload organization) {
        return jdbcClient.sql("""
                        select u.id,
                               u.email,
                               u.display_name,
                               u.status,
                               cm.role,
                               cm.status as membership_status,
                               cm.primary_membership,
                               u.created_at,
                               u.updated_at
                          from app_users u
                          join organization_memberships cm on cm.user_id = u.id
                          join organizations c on c.id = cm.organization_id
                         where cm.organization_id = :organizationId
                           and c.workspace_id = :workspaceId
                         order by u.created_at asc, u.email asc
                        """)
                .param("organizationId", organizationId(organization))
                .param("workspaceId", workspaceId(organization))
                .query(this::mapUser)
                .list();
    }

    private AdminUserSummaryPayload requireUser(OrganizationContextPayload organization, UUID userId) {
        return jdbcClient.sql("""
                        select u.id,
                               u.email,
                               u.display_name,
                               u.status,
                               cm.role,
                               cm.status as membership_status,
                               cm.primary_membership,
                               u.created_at,
                               u.updated_at
                          from app_users u
                          join organization_memberships cm on cm.user_id = u.id
                          join organizations c on c.id = cm.organization_id
                         where u.id = :userId
                           and cm.organization_id = :organizationId
                           and c.workspace_id = :workspaceId
                        """)
                .param("userId", userId)
                .param("organizationId", organizationId(organization))
                .param("workspaceId", workspaceId(organization))
                .query(this::mapUser)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("User was not found in the active organization context."));
    }

    private UUID findUserIdByEmail(String email) {
        return jdbcClient.sql("select id from app_users where email = :email")
                .param("email", normalizeEmail(email))
                .query(UUID.class)
                .optional()
                .orElse(null);
    }

    private OrganizationContextPayload requireCurrentOrganization(AuthPrincipal principal) {
        return currentUserContextService.findCurrentOrganizationContext(principal)
                .orElseThrow(() -> new AccessDeniedException("An active organization context is required."));
    }

    private ValidatedUserInput validateCreateInput(AuthPrincipal principal, CreateAdminUserInput input) {
        if (input == null) {
            throw new IllegalArgumentException("User input is required.");
        }
        String role = requireRole(principal, input.role());
        return new ValidatedUserInput(
                requireEmail(input.email()),
                requireText("Display name", input.displayName(), 160),
                requireAllowed("User status", input.userStatus(), USER_CREATE_STATUSES),
                role,
                requireAllowed("Membership status", input.membershipStatus(), MEMBERSHIP_CREATE_STATUSES),
                Boolean.TRUE.equals(input.primaryMembership()));
    }

    private ValidatedUserUpdateInput validateUpdateInput(AuthPrincipal principal, UpdateAdminUserInput input) {
        if (input == null) {
            throw new IllegalArgumentException("User input is required.");
        }
        String role = requireRole(principal, input.role());
        return new ValidatedUserUpdateInput(
                requireText("Display name", input.displayName(), 160),
                requireAllowed("User status", input.userStatus(), USER_STATUSES),
                role,
                requireAllowed("Membership status", input.membershipStatus(), MEMBERSHIP_STATUSES),
                Boolean.TRUE.equals(input.primaryMembership()));
    }

    private String requireRole(AuthPrincipal principal, String value) {
        String role = requireAllowed("Role", value, USER_ROLES);
        if ("SUPERADMIN".equals(role) && !principal.roles().contains("SUPERADMIN")) {
            throw new AccessDeniedException("Only a SUPERADMIN can assign the SUPERADMIN role.");
        }
        return role;
    }

    private String requireEmail(String value) {
        String email = normalizeEmail(value);
        if (!StringUtils.hasText(email) || !email.contains("@") || email.length() > 320) {
            throw new IllegalArgumentException("A valid email is required.");
        }
        return email;
    }

    private String requireText(String fieldName, String value, int maxLength) {
        String normalized = StringUtils.trimWhitespace(value);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return normalized;
    }

    private String requireAllowed(String fieldName, String value, Set<String> allowedValues) {
        String normalized = StringUtils.trimWhitespace(value).toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new IllegalArgumentException(fieldName + " is unsupported.");
        }
        return normalized;
    }

    private UUID parseRequiredUuid(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " must be a UUID.", ex);
        }
    }

    private UUID organizationId(OrganizationContextPayload organization) {
        return UUID.fromString(organization.organizationId());
    }

    private UUID workspaceId(OrganizationContextPayload organization) {
        return UUID.fromString(organization.workspaceId());
    }

    private String normalizeEmail(String email) {
        return StringUtils.trimWhitespace(email).toLowerCase(Locale.ROOT);
    }

    private AdminUserSummaryPayload mapUser(ResultSet rs, int rowNumber) throws SQLException {
        return new AdminUserSummaryPayload(
                rs.getString("id"),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("status"),
                rs.getString("role"),
                rs.getString("membership_status"),
                rs.getBoolean("primary_membership"),
                timestampToString(rs, "created_at"),
                timestampToString(rs, "updated_at"));
    }

    private String timestampToString(ResultSet rs, String columnName) throws SQLException {
        OffsetDateTime timestamp = rs.getObject(columnName, OffsetDateTime.class);
        return timestamp == null ? null : timestamp.toString();
    }

    private record ValidatedUserInput(
            String email,
            String displayName,
            String userStatus,
            String role,
            String membershipStatus,
            boolean primaryMembership) {
    }

    private record ValidatedUserUpdateInput(
            String displayName,
            String userStatus,
            String role,
            String membershipStatus,
            boolean primaryMembership) {
    }
}
