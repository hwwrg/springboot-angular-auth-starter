package com.example.authstarter.auth;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
public class UserInvitationService {

    private static final String INVITATION_PURPOSE = "INVITATION";
    private static final Duration INVITATION_TTL = Duration.ofDays(7);
    private static final int TOKEN_BYTES = 32;

    private final JdbcClient jdbcClient;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserInvitationService(
            JdbcClient jdbcClient,
            PasswordEncoder passwordEncoder) {
        this.jdbcClient = jdbcClient;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserInvitationTokenPayload createInvitation(AuthPrincipal createdBy, UUID userId) {
        String rawToken = generateRawToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(INVITATION_TTL);
        UUID tokenId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into user_security_tokens (
                            id,
                            user_id,
                            purpose,
                            token_hash,
                            token_hash_algorithm,
                            expires_at,
                            created_by_user_id
                        ) values (
                            :id,
                            :userId,
                            :purpose,
                            :tokenHash,
                            'SHA256',
                            now() + (:ttlSeconds * interval '1 second'),
                            :createdByUserId
                        )
                        """)
                .param("id", tokenId)
                .param("userId", userId)
                .param("purpose", INVITATION_PURPOSE)
                .param("tokenHash", SecurityTokenHasher.sha256(rawToken))
                .param("ttlSeconds", INVITATION_TTL.toSeconds())
                .param("createdByUserId", createdByUserId(createdBy))
                .update();

        return new UserInvitationTokenPayload(tokenId, userId, rawToken, expiresAt, INVITATION_TTL.toDays());
    }

    @Transactional
    public InvitationPasswordSetupPayload acceptInvitation(AcceptUserInviteInput input) {
        String token = StringUtils.trimWhitespace(input == null ? null : input.token());
        String newPassword = input == null ? null : input.newPassword();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(newPassword)) {
            throw invalidInvite();
        }

        InvitationToken tokenRecord = findInvitationToken(SecurityTokenHasher.sha256(token));
        if (tokenRecord == null
                || tokenRecord.consumedAt() != null
                || tokenRecord.revokedAt() != null
                || !tokenRecord.expiresAt().isAfter(OffsetDateTime.now())
                || !List.of("INVITED", "ACTIVE").contains(tokenRecord.userStatus())
                || !List.of("INVITED", "ACTIVE").contains(tokenRecord.membershipStatus())
                || tokenRecord.credentialExists()
                || !"ACTIVE".equals(tokenRecord.organizationStatus())
                || !"ACTIVE".equals(tokenRecord.workspaceStatus())) {
            throw invalidInvite();
        }

        int consumedRows = jdbcClient.sql("""
                        update user_security_tokens
                           set consumed_at = now(),
                               updated_at = now()
                         where id = :tokenId
                           and purpose = 'INVITATION'
                           and consumed_at is null
                           and revoked_at is null
                           and expires_at > now()
                        """)
                .param("tokenId", tokenRecord.tokenId())
                .update();
        if (consumedRows != 1) {
            throw invalidInvite();
        }

        jdbcClient.sql("""
                        insert into user_credentials (
                            user_id,
                            password_hash,
                            password_algorithm,
                            password_changed_at,
                            must_change_password,
                            email_verified_at,
                            failed_login_count,
                            locked_until
                        ) values (
                            :userId,
                            :passwordHash,
                            'BCRYPT',
                            now(),
                            false,
                            now(),
                            0,
                            null
                        )
                        """)
                .param("userId", tokenRecord.userId())
                .param("passwordHash", passwordEncoder.encode(newPassword))
                .update();

        jdbcClient.sql("""
                        update app_users
                           set status = 'ACTIVE',
                               updated_at = now()
                         where id = :userId
                           and status = 'INVITED'
                        """)
                .param("userId", tokenRecord.userId())
                .update();
        jdbcClient.sql("""
                        update organization_memberships
                           set status = 'ACTIVE',
                               updated_at = now()
                         where id = :membershipId
                           and status = 'INVITED'
                        """)
                .param("membershipId", tokenRecord.membershipId())
                .update();

        return new InvitationPasswordSetupPayload(
                tokenRecord.userId().toString(),
                tokenRecord.email(),
                "ACTIVE");
    }

    private InvitationToken findInvitationToken(String tokenHash) {
        return jdbcClient.sql("""
                        select ust.id as token_id,
                               ust.user_id,
                               ust.expires_at,
                               ust.consumed_at,
                               ust.revoked_at,
                               u.email,
                               u.display_name,
                               u.status as user_status,
                               cm.id as membership_id,
                               cm.role,
                               cm.status as membership_status,
                               uc.user_id is not null as credential_exists,
                               c.id as organization_id,
                               c.display_name as organization_display_name,
                               c.status as organization_status,
                               tb.id as workspace_id,
                               tb.code as workspace_code,
                               tb.status as workspace_status
                          from user_security_tokens ust
                          join app_users u on u.id = ust.user_id
                          join organization_memberships cm on cm.user_id = u.id
                          left join user_credentials uc on uc.user_id = u.id
                          join organizations c on c.id = cm.organization_id
                          join workspaces tb on tb.id = c.workspace_id
                         where ust.purpose = 'INVITATION'
                           and ust.token_hash = :tokenHash
                           and ust.token_hash_algorithm = 'SHA256'
                         order by cm.primary_membership desc, cm.created_at asc
                         limit 1
                        """)
                .param("tokenHash", tokenHash)
                .query(this::mapInvitationToken)
                .optional()
                .orElse(null);
    }

    private InvitationToken mapInvitationToken(ResultSet rs, int rowNumber) throws SQLException {
        return new InvitationToken(
                UUID.fromString(rs.getString("token_id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getObject("expires_at", OffsetDateTime.class),
                rs.getObject("consumed_at", OffsetDateTime.class),
                rs.getObject("revoked_at", OffsetDateTime.class),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("user_status"),
                UUID.fromString(rs.getString("membership_id")),
                rs.getString("role"),
                rs.getString("membership_status"),
                rs.getBoolean("credential_exists"),
                UUID.fromString(rs.getString("organization_id")),
                rs.getString("organization_display_name"),
                rs.getString("organization_status"),
                UUID.fromString(rs.getString("workspace_id")),
                rs.getString("workspace_code"),
                rs.getString("workspace_status"));
    }

    private String generateRawToken() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private UUID createdByUserId(AuthPrincipal principal) {
        if (principal == null || !StringUtils.hasText(principal.id())) {
            return null;
        }
        try {
            return UUID.fromString(principal.id());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private BadCredentialsException invalidInvite() {
        return new BadCredentialsException("Invitation token is invalid or expired.");
    }

    private record InvitationToken(
            UUID tokenId,
            UUID userId,
            OffsetDateTime expiresAt,
            OffsetDateTime consumedAt,
            OffsetDateTime revokedAt,
            String email,
            String displayName,
            String userStatus,
            UUID membershipId,
            String role,
            String membershipStatus,
            boolean credentialExists,
            UUID organizationId,
            String organizationDisplayName,
            String organizationStatus,
            UUID workspaceId,
            String workspaceCode,
            String workspaceStatus) {
    }
}
