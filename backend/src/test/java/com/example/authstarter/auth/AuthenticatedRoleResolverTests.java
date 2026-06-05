package com.example.authstarter.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.authstarter.security.AuthStarterRole;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedRoleResolverTests {

    private final AuthenticatedRoleResolver roleResolver = new AuthenticatedRoleResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesCanonicalRolesFromAuthenticatedSessionPrincipal() {
        AuthPrincipal principal = new AuthPrincipal(
                "baseline-operator",
                "operator@example.test",
                "Test Operator",
                List.of("SUPERADMIN"));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(roleResolver.resolveCurrentRoles()).isEqualTo(Set.of(AuthStarterRole.SUPERADMIN));
    }

    @Test
    void returnsNoRolesWhenSessionIsAnonymous() {
        assertThat(roleResolver.resolveCurrentRoles()).isEmpty();
    }
}
