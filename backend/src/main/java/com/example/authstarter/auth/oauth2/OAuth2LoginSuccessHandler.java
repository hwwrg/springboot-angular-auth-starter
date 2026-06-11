package com.example.authstarter.auth.oauth2;

import com.example.authstarter.auth.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Completes an OAuth2/OIDC login by replacing the provider authentication with
 * the same session-backed {@link AuthPrincipal} authentication produced by the
 * credential login flow. Identities whose verified email does not match an
 * active local account are rejected and redirected back to the login page.
 */
@Component
@EnableConfigurationProperties(OAuth2LoginProperties.class)
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final OAuth2LoginProperties properties;
    private final ObjectProvider<OAuth2AccountLinkingService> accountLinkingService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public OAuth2LoginSuccessHandler(
            OAuth2LoginProperties properties,
            ObjectProvider<OAuth2AccountLinkingService> accountLinkingService) {
        this.properties = properties;
        this.accountLinkingService = accountLinkingService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {
        Optional<AuthPrincipal> principal = verifiedEmail(authentication)
                .flatMap(email -> accountLinkingService.stream()
                        .findFirst()
                        .flatMap(service -> service.linkByEmail(email)));

        if (principal.isEmpty()) {
            rejectUnlinkedLogin(request, response);
            return;
        }

        Authentication sessionAuthentication = new UsernamePasswordAuthenticationToken(
                principal.get(),
                null,
                principal.get().roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList());

        SecurityContext securityContext = securityContextHolderStrategy.createEmptyContext();
        securityContext.setAuthentication(sessionAuthentication);
        securityContextHolderStrategy.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);

        response.sendRedirect(properties.successRedirectUrl());
    }

    /**
     * Returns the provider email only when it can be trusted: missing emails
     * are rejected, and providers that report verification (OIDC
     * {@code email_verified}) must not flag the address as unverified.
     */
    private Optional<String> verifiedEmail(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token) || token.getPrincipal() == null) {
            return Optional.empty();
        }

        Object email = token.getPrincipal().getAttributes().get("email");
        if (!(email instanceof String emailValue) || emailValue.isBlank()) {
            LOGGER.info(
                    "Rejected OAuth2 login from registration {} because the provider returned no email.",
                    token.getAuthorizedClientRegistrationId());
            return Optional.empty();
        }

        Object verified = token.getPrincipal().getAttributes().get("email_verified");
        boolean unverified = Boolean.FALSE.equals(verified) || "false".equals(verified);
        if (unverified) {
            LOGGER.info(
                    "Rejected OAuth2 login from registration {} because the provider email is unverified.",
                    token.getAuthorizedClientRegistrationId());
            return Optional.empty();
        }

        return Optional.of(emailValue);
    }

    private void rejectUnlinkedLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContext emptyContext = securityContextHolderStrategy.createEmptyContext();
        securityContextHolderStrategy.setContext(emptyContext);
        securityContextRepository.saveContext(emptyContext, request, response);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        response.sendRedirect(redirectWithError(properties.failureRedirectUrl(), "oauth2-unlinked"));
    }

    static String redirectWithError(String baseUrl, String errorCode) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "error=" + errorCode;
    }
}
