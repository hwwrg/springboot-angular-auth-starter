package com.example.authstarter.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.authstarter.auth.mfa.UserMfaService;
import com.example.authstarter.foundation.CurrentUserContextService;
import com.example.authstarter.security.AuthStarterRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit-level coverage of the login MFA gate: a credential success for an
 * MFA-enabled account is held pending until a second factor is verified.
 */
class BaselineAuthServiceMfaTests {

    private static final UUID USER_ID = UUID.fromString("4f6b2c10-0000-4000-8000-00000000aa01");

    private MockHttpServletRequest request;
    private UserCredentialAuthenticationService credentialService;
    private UserMfaService mfaService;
    private BaselineAuthService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        BaselineAuthProperties properties = new BaselineAuthProperties(
                "", "", "Baseline Operator", AuthStarterRole.USER, false, List.of());
        CurrentUserContextService currentUserContextService = mock(CurrentUserContextService.class);

        credentialService = mock(UserCredentialAuthenticationService.class);
        AuthPrincipal principal = new AuthPrincipal(USER_ID.toString(), "user@example.test", "MFA User", List.of("USER"));
        when(credentialService.authenticate(any())).thenReturn(Optional.of(principal));
        ObjectProvider<UserCredentialAuthenticationService> credentialProvider = mock(ObjectProvider.class);
        when(credentialProvider.stream()).thenAnswer(invocation -> Stream.of(credentialService));

        mfaService = mock(UserMfaService.class);
        when(mfaService.isMfaEnabled(USER_ID)).thenReturn(true);
        ObjectProvider<UserMfaService> mfaProvider = mock(ObjectProvider.class);
        when(mfaProvider.getIfAvailable()).thenReturn(mfaService);

        service = new BaselineAuthService(properties, currentUserContextService, credentialProvider, mfaProvider);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void heldsLoginPendingASecondFactorForMfaEnabledAccounts() {
        AuthSessionPayload payload = service.login(new LoginInput("user@example.test", "correct-password"));

        assertThat(payload.mfaRequired()).isTrue();
        assertThat(payload.authenticated()).isFalse();
        assertThat(payload.principal()).isNull();
        // No authenticated security context is established yet.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getSession(false)).isNotNull();
    }

    @Test
    void completesLoginWhenTheSecondFactorIsVerified() {
        service.login(new LoginInput("user@example.test", "correct-password"));
        when(mfaService.verifyChallenge(eq(USER_ID), eq("123456"))).thenReturn(true);

        AuthSessionPayload payload = service.verifyMfa(new VerifyMfaInput("123456"));

        assertThat(payload.authenticated()).isTrue();
        assertThat(payload.mfaRequired()).isFalse();
        assertThat(payload.principal().email()).isEqualTo("user@example.test");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(AuthPrincipal.class);
    }

    @Test
    void rejectsAnIncorrectSecondFactorAndKeepsTheChallengePending() {
        service.login(new LoginInput("user@example.test", "correct-password"));
        when(mfaService.verifyChallenge(eq(USER_ID), any())).thenReturn(false);

        assertThatThrownBy(() -> service.verifyMfa(new VerifyMfaInput("000000")))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // The pending challenge survives a wrong attempt, so a subsequent correct code still works.
        when(mfaService.verifyChallenge(eq(USER_ID), eq("123456"))).thenReturn(true);
        AuthSessionPayload payload = service.verifyMfa(new VerifyMfaInput("123456"));
        assertThat(payload.authenticated()).isTrue();
    }

    @Test
    void rejectsVerificationWhenNoChallengeIsInProgress() {
        assertThatThrownBy(() -> service.verifyMfa(new VerifyMfaInput("123456")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
