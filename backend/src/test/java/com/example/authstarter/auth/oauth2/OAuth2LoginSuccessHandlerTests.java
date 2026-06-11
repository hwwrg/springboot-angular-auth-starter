package com.example.authstarter.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.authstarter.auth.AuthPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class OAuth2LoginSuccessHandlerTests {

    private static final String SUCCESS_URL = "http://localhost:4200/app/dashboard";
    private static final String FAILURE_URL = "http://localhost:4200/login";

    private OAuth2AccountLinkingService accountLinkingService;
    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void createHandler() {
        accountLinkingService = mock(OAuth2AccountLinkingService.class);
        ObjectProvider<OAuth2AccountLinkingService> provider = mock(ObjectProvider.class);
        when(provider.stream()).thenAnswer(invocation -> Stream.of(accountLinkingService));
        successHandler = new OAuth2LoginSuccessHandler(
                new OAuth2LoginProperties(SUCCESS_URL, FAILURE_URL),
                provider);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void establishesSessionPrincipalAndRedirectsWhenEmailMatchesActiveAccount() throws Exception {
        AuthPrincipal linkedPrincipal = new AuthPrincipal(
                "5f0e8a3c-0000-0000-0000-000000000001",
                "user@example.test",
                "Linked User",
                List.of("USER"));
        when(accountLinkingService.linkByEmail("user@example.test")).thenReturn(Optional.of(linkedPrincipal));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                request,
                response,
                oauthToken(Map.of("email", "user@example.test", "email_verified", true)));

        assertThat(response.getRedirectedUrl()).isEqualTo(SUCCESS_URL);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext.getAuthentication().getPrincipal()).isEqualTo(linkedPrincipal);
        assertThat(securityContext.getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_USER");
        assertThat(request.getSession(false)).isNotNull();
    }

    @Test
    void rejectsLoginWhenNoActiveAccountMatchesTheEmail() throws Exception {
        when(accountLinkingService.linkByEmail(anyString())).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                request,
                response,
                oauthToken(Map.of("email", "unknown@example.test")));

        assertThat(response.getRedirectedUrl()).isEqualTo(FAILURE_URL + "?error=oauth2-unlinked");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsLoginWhenProviderReturnsNoEmail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(request, response, oauthToken(Map.of("name", "No Email")));

        assertThat(response.getRedirectedUrl()).isEqualTo(FAILURE_URL + "?error=oauth2-unlinked");
        Mockito.verifyNoInteractions(accountLinkingService);
    }

    @Test
    void rejectsLoginWhenProviderEmailIsUnverified() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                request,
                response,
                oauthToken(Map.of("email", "user@example.test", "email_verified", false)));

        assertThat(response.getRedirectedUrl()).isEqualTo(FAILURE_URL + "?error=oauth2-unlinked");
        Mockito.verifyNoInteractions(accountLinkingService);
    }

    private OAuth2AuthenticationToken oauthToken(Map<String, Object> attributes) {
        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                attributes,
                attributes.keySet().iterator().next());
        return new OAuth2AuthenticationToken(
                oauthUser,
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                "test-provider");
    }
}
