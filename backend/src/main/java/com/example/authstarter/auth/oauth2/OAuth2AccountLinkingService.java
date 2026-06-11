package com.example.authstarter.auth.oauth2;

import com.example.authstarter.auth.AuthPrincipal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Maps a verified external identity to an existing local account. Linking is
 * intentionally restricted to ACTIVE users with at least one active
 * membership: OAuth2 login never provisions accounts, so invitation and RBAC
 * lifecycle rules keep applying to externally authenticated users.
 */
@Service
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
public class OAuth2AccountLinkingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AccountLinkingService.class);

    private final JdbcClient jdbcClient;

    public OAuth2AccountLinkingService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<AuthPrincipal> linkByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<LinkedUser> user = jdbcClient.sql("""
                        select id, email, display_name
                          from app_users
                         where email = :email
                           and status = 'ACTIVE'
                        """)
                .param("email", normalizedEmail)
                .query((rs, rowNumber) -> new LinkedUser(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("display_name")))
                .optional();
        if (user.isEmpty()) {
            LOGGER.info("Rejected OAuth2 login for {} because no active local account matches.", normalizedEmail);
            return Optional.empty();
        }

        List<String> activeRoles = activeRoles(user.get().id());
        if (activeRoles.isEmpty()) {
            LOGGER.info(
                    "Rejected OAuth2 login for {} because no active membership/organization/workspace was found.",
                    normalizedEmail);
            return Optional.empty();
        }

        recordSuccessfulLogin(user.get().id());
        return Optional.of(new AuthPrincipal(
                user.get().id().toString(),
                user.get().email(),
                user.get().displayName(),
                activeRoles));
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

    private record LinkedUser(UUID id, String email, String displayName) {
    }
}
