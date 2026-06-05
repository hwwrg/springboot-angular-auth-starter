package com.example.authstarter.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
public class UserCredentialAuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCredentialAuthenticationService.class);
    private static final int MAX_FAILED_LOGIN_COUNT = 5;
    private static final int LOCKOUT_SECONDS = 15 * 60;

    private final JdbcClient jdbcClient;
    private final PasswordEncoder passwordEncoder;

    public UserCredentialAuthenticationService(JdbcClient jdbcClient, PasswordEncoder passwordEncoder) {
        this.jdbcClient = jdbcClient;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<AuthPrincipal> authenticate(LoginInput input) {
        String email = normalizeEmail(input.email());
        Optional<UserCredentialCandidate> candidate = findCredentialCandidate(email);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        UserCredentialCandidate user = candidate.get();
        if (!"ACTIVE".equals(user.userStatus())) {
            LOGGER.info("Rejected DB-backed login for non-active user {} with status {}.", email, user.userStatus());
            throw invalidCredentials();
        }

        if (!StringUtils.hasText(user.passwordHash())) {
            return Optional.empty();
        }

        if (!"BCRYPT".equals(user.passwordAlgorithm())) {
            LOGGER.warn(
                    "Rejected DB-backed login for {} because password algorithm {} is not supported yet.",
                    email,
                    user.passwordAlgorithm());
            throw invalidCredentials();
        }

        if (isLocked(user.lockedUntil())) {
            LOGGER.info("Rejected DB-backed login for locked user {}.", email);
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(input.password(), user.passwordHash())) {
            recordFailedLogin(user.userId());
            throw invalidCredentials();
        }

        List<String> activeRoles = activeRoles(user.userId());
        if (activeRoles.isEmpty()) {
            LOGGER.info("Rejected DB-backed login for {} because no active membership/organization/workspace was found.", email);
            throw invalidCredentials();
        }

        recordSuccessfulLogin(user.userId());
        return Optional.of(new AuthPrincipal(
                user.userId().toString(),
                user.email(),
                user.displayName(),
                activeRoles,
                user.mustChangePassword()));
    }

    public AuthPrincipal changeOwnPassword(AuthPrincipal principal, ChangeOwnPasswordInput input) {
        String newPassword = input == null ? null : input.newPassword();
        if (!StringUtils.hasText(newPassword)) {
            throw invalidCredentials();
        }

        UUID userId = UUID.fromString(principal.id());
        if (!principal.mustChangePassword()) {
            String currentPassword = input.currentPassword();
            if (!StringUtils.hasText(currentPassword) || !currentPasswordMatches(userId, currentPassword)) {
                throw invalidCredentials();
            }
        }

        int updatedRows = jdbcClient.sql("""
                        update user_credentials
                           set password_hash = :passwordHash,
                               password_algorithm = 'BCRYPT',
                               password_changed_at = now(),
                               must_change_password = false,
                               failed_login_count = 0,
                               locked_until = null,
                               updated_at = now()
                         where user_id = :userId
                        """)
                .param("userId", userId)
                .param("passwordHash", passwordEncoder.encode(newPassword))
                .update();
        if (updatedRows != 1) {
            throw invalidCredentials();
        }

        List<String> activeRoles = activeRoles(userId);
        if (activeRoles.isEmpty()) {
            throw invalidCredentials();
        }

        return new AuthPrincipal(
                principal.id(),
                principal.email(),
                principal.displayName(),
                activeRoles,
                false);
    }

    private boolean currentPasswordMatches(UUID userId, String currentPassword) {
        CurrentCredential credential = jdbcClient.sql("""
                        select password_hash,
                               password_algorithm,
                               locked_until
                          from user_credentials
                         where user_id = :userId
                        """)
                .param("userId", userId)
                .query((rs, rowNumber) -> new CurrentCredential(
                        rs.getString("password_hash"),
                        rs.getString("password_algorithm"),
                        rs.getObject("locked_until", OffsetDateTime.class)))
                .optional()
                .orElseThrow(this::invalidCredentials);

        if (!StringUtils.hasText(credential.passwordHash())
                || !"BCRYPT".equals(credential.passwordAlgorithm())
                || isLocked(credential.lockedUntil())) {
            return false;
        }
        return passwordEncoder.matches(currentPassword, credential.passwordHash());
    }

    private Optional<UserCredentialCandidate> findCredentialCandidate(String email) {
        return jdbcClient.sql("""
                        select u.id as user_id,
                               u.email,
                               u.display_name,
                               u.status as user_status,
                               uc.password_hash,
                               uc.password_algorithm,
                               uc.must_change_password,
                               uc.locked_until
                          from app_users u
                          left join user_credentials uc on uc.user_id = u.id
                         where u.email = :email
                        """)
                .param("email", email)
                .query(this::mapCandidate)
                .optional();
    }

    private UserCredentialCandidate mapCandidate(ResultSet rs, int rowNumber) throws SQLException {
        return new UserCredentialCandidate(
                UUID.fromString(rs.getString("user_id")),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("user_status"),
                rs.getString("password_hash"),
                rs.getString("password_algorithm"),
                rs.getBoolean("must_change_password"),
                rs.getObject("locked_until", OffsetDateTime.class));
    }

    private List<String> activeRoles(UUID userId) {
        return jdbcClient.sql("""
                        select distinct cm.role
                          from app_users u
                          join organization_memberships cm on cm.user_id = u.id
                          join organizations c on c.id = cm.organization_id
                          join workspaces tb on tb.id = c.workspace_id
                         where u.id = :userId
                           and u.status = 'ACTIVE'
                           and cm.status = 'ACTIVE'
                           and c.status = 'ACTIVE'
                           and tb.status = 'ACTIVE'
                         order by cm.role
                        """)
                .param("userId", userId)
                .query(String.class)
                .list();
    }

    private void recordSuccessfulLogin(UUID userId) {
        jdbcClient.sql("""
                        update user_credentials
                           set failed_login_count = 0,
                               locked_until = null,
                               last_login_at = now(),
                               updated_at = now()
                         where user_id = :userId
                        """)
                .param("userId", userId)
                .update();
    }

    private void recordFailedLogin(UUID userId) {
        jdbcClient.sql("""
                        with next_state as (
                            select user_id,
                                   case
                                       when locked_until is not null and locked_until <= now() then 1
                                       else failed_login_count + 1
                                   end as next_failed_login_count
                              from user_credentials
                             where user_id = :userId
                        )
                        update user_credentials uc
                           set failed_login_count = next_state.next_failed_login_count,
                               locked_until = case
                                   when next_state.next_failed_login_count >= :maxFailedLoginCount
                                       then now() + (:lockoutSeconds * interval '1 second')
                                   else null
                               end,
                               updated_at = now()
                          from next_state
                         where uc.user_id = next_state.user_id
                        """)
                .param("userId", userId)
                .param("maxFailedLoginCount", MAX_FAILED_LOGIN_COUNT)
                .param("lockoutSeconds", LOCKOUT_SECONDS)
                .update();
    }

    private boolean isLocked(OffsetDateTime lockedUntil) {
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }

    private String normalizeEmail(String email) {
        return StringUtils.trimWhitespace(email).toLowerCase(Locale.ROOT);
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Invalid email or password.");
    }

    private record UserCredentialCandidate(
            UUID userId,
            String email,
            String displayName,
            String userStatus,
            String passwordHash,
            String passwordAlgorithm,
            boolean mustChangePassword,
            OffsetDateTime lockedUntil) {
    }

    private record CurrentCredential(String passwordHash, String passwordAlgorithm, OffsetDateTime lockedUntil) {
    }
}
