package com.example.authstarter.auth;

import com.example.authstarter.foundation.OrganizationContextPayload;
import com.example.authstarter.notification.AccountNotificationDeliveryException;
import com.example.authstarter.notification.NotificationService;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String PASSWORD_RESET_PURPOSE = "PASSWORD_RESET";
    private static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);
    private static final int TOKEN_BYTES = 32;
    private static final String GENERIC_MESSAGE =
            "If a matching active account exists, a password reset email has been sent.";

    private final JdbcClient jdbcClient;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetUrlBuilder passwordResetUrlBuilder;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            JdbcClient jdbcClient,
            PasswordEncoder passwordEncoder,
            PasswordResetUrlBuilder passwordResetUrlBuilder,
            NotificationService notificationService) {
        this.jdbcClient = jdbcClient;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetUrlBuilder = passwordResetUrlBuilder;
        this.notificationService = notificationService;
    }

    public PasswordResetPayload requestReset(PasswordResetRequestInput input) {
        String email = normalizeEmail(input == null ? null : input.email());
        if (!StringUtils.hasText(email)) {
            return new PasswordResetPayload(GENERIC_MESSAGE);
        }

        ActivePasswordResetUser user = findActiveDbBackedUser(email);
        if (user == null) {
            return new PasswordResetPayload(GENERIC_MESSAGE);
        }

        String rawToken = generateRawToken();
        UUID tokenId = UUID.randomUUID();
        revokeExistingActiveResetTokens(user.userId());
        jdbcClient.sql("""
                        insert into user_security_tokens (
                            id,
                            user_id,
                            purpose,
                            token_hash,
                            token_hash_algorithm,
                            expires_at
                        ) values (
                            :id,
                            :userId,
                            :purpose,
                            :tokenHash,
                            'SHA256',
                            now() + (:ttlSeconds * interval '1 second')
                        )
                        """)
                .param("id", tokenId)
                .param("userId", user.userId())
                .param("purpose", PASSWORD_RESET_PURPOSE)
                .param("tokenHash", SecurityTokenHasher.sha256(rawToken))
                .param("ttlSeconds", PASSWORD_RESET_TTL.toSeconds())
                .update();

        try {
            notificationService.sendPasswordReset(
                    user.organizationContext(),
                    user.userId(),
                    user.displayName(),
                    user.email(),
                    passwordResetUrlBuilder.resetUrl(rawToken));
        } catch (AccountNotificationDeliveryException | IllegalArgumentException ex) {
            LOGGER.warn("Password reset email delivery failed for user {}.", user.userId());
        }

        return new PasswordResetPayload(GENERIC_MESSAGE);
    }

    @Transactional
    public PasswordResetPayload completeReset(PasswordResetCompleteInput input) {
        String token = StringUtils.trimWhitespace(input == null ? null : input.token());
        String newPassword = input == null ? null : input.newPassword();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(newPassword)) {
            throw invalidResetToken();
        }

        PasswordResetToken tokenRecord = findPasswordResetToken(SecurityTokenHasher.sha256(token));
        if (tokenRecord == null
                || tokenRecord.consumedAt() != null
                || tokenRecord.revokedAt() != null
                || !tokenRecord.expiresAt().isAfter(OffsetDateTime.now())
                || !"ACTIVE".equals(tokenRecord.userStatus())) {
            throw invalidResetToken();
        }

        int consumedRows = jdbcClient.sql("""
                        update user_security_tokens
                           set consumed_at = now(),
                               updated_at = now()
                         where id = :tokenId
                           and purpose = 'PASSWORD_RESET'
                           and consumed_at is null
                           and revoked_at is null
                           and expires_at > now()
                        """)
                .param("tokenId", tokenRecord.tokenId())
                .update();
        if (consumedRows != 1) {
            throw invalidResetToken();
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
                .param("userId", tokenRecord.userId())
                .param("passwordHash", passwordEncoder.encode(newPassword))
                .update();
        if (updatedRows != 1) {
            throw invalidResetToken();
        }

        return new PasswordResetPayload("Password has been reset. You can now sign in.");
    }

    private ActivePasswordResetUser findActiveDbBackedUser(String email) {
        return jdbcClient.sql("""
                        select u.id as user_id,
                               u.email,
                               u.display_name,
                               c.id as organization_id,
                               c.display_name as organization_display_name,
                               c.status as organization_status,
                               tb.id as workspace_id,
                               tb.code as workspace_code,
                               tb.status as workspace_status,
                               cm.role
                          from app_users u
                          join user_credentials uc on uc.user_id = u.id
                          join organization_memberships cm on cm.user_id = u.id
                          join organizations c on c.id = cm.organization_id
                          join workspaces tb on tb.id = c.workspace_id
                         where u.email = :email
                           and u.status = 'ACTIVE'
                           and uc.password_hash is not null
                           and cm.status = 'ACTIVE'
                           and c.status = 'ACTIVE'
                           and tb.status = 'ACTIVE'
                         order by cm.primary_membership desc, cm.created_at asc
                         limit 1
                        """)
                .param("email", email)
                .query(this::mapActivePasswordResetUser)
                .optional()
                .orElse(null);
    }

    private ActivePasswordResetUser mapActivePasswordResetUser(ResultSet rs, int rowNumber) throws SQLException {
        OrganizationContextPayload organization = new OrganizationContextPayload(
                rs.getString("organization_id"),
                rs.getString("organization_display_name"),
                rs.getString("organization_status"),
                rs.getString("workspace_id"),
                rs.getString("workspace_code"),
                rs.getString("workspace_status"),
                rs.getString("role"));
        return new ActivePasswordResetUser(
                UUID.fromString(rs.getString("user_id")),
                rs.getString("email"),
                rs.getString("display_name"),
                organization);
    }

    private PasswordResetToken findPasswordResetToken(String tokenHash) {
        return jdbcClient.sql("""
                        select ust.id as token_id,
                               ust.user_id,
                               ust.expires_at,
                               ust.consumed_at,
                               ust.revoked_at,
                               u.status as user_status
                          from user_security_tokens ust
                          join app_users u on u.id = ust.user_id
                         where ust.purpose = 'PASSWORD_RESET'
                           and ust.token_hash = :tokenHash
                           and ust.token_hash_algorithm = 'SHA256'
                         limit 1
                        """)
                .param("tokenHash", tokenHash)
                .query(this::mapPasswordResetToken)
                .optional()
                .orElse(null);
    }

    private PasswordResetToken mapPasswordResetToken(ResultSet rs, int rowNumber) throws SQLException {
        return new PasswordResetToken(
                UUID.fromString(rs.getString("token_id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getObject("expires_at", OffsetDateTime.class),
                rs.getObject("consumed_at", OffsetDateTime.class),
                rs.getObject("revoked_at", OffsetDateTime.class),
                rs.getString("user_status"));
    }

    private void revokeExistingActiveResetTokens(UUID userId) {
        jdbcClient.sql("""
                        update user_security_tokens
                           set revoked_at = now(),
                               updated_at = now()
                         where user_id = :userId
                           and purpose = 'PASSWORD_RESET'
                           and consumed_at is null
                           and revoked_at is null
                        """)
                .param("userId", userId)
                .update();
    }

    private String generateRawToken() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String normalizeEmail(String email) {
        return StringUtils.trimWhitespace(email).toLowerCase(Locale.ROOT);
    }

    private BadCredentialsException invalidResetToken() {
        return new BadCredentialsException("Password reset token is invalid or expired.");
    }

    private record ActivePasswordResetUser(
            UUID userId,
            String email,
            String displayName,
            OrganizationContextPayload organizationContext) {
    }

    private record PasswordResetToken(
            UUID tokenId,
            UUID userId,
            OffsetDateTime expiresAt,
            OffsetDateTime consumedAt,
            OffsetDateTime revokedAt,
            String userStatus) {
    }
}
