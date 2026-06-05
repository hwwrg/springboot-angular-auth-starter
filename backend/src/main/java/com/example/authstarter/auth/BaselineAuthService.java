package com.example.authstarter.auth;

import com.example.authstarter.foundation.CurrentUserContextService;
import com.example.authstarter.foundation.UserSessionIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class BaselineAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaselineAuthService.class);

    private final BaselineAuthProperties properties;
    private final CurrentUserContextService currentUserContextService;
    private final ObjectProvider<UserCredentialAuthenticationService> userCredentialAuthenticationService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public BaselineAuthService(
            BaselineAuthProperties properties,
            CurrentUserContextService currentUserContextService,
            ObjectProvider<UserCredentialAuthenticationService> userCredentialAuthenticationService) {
        this.properties = properties;
        this.currentUserContextService = currentUserContextService;
        this.userCredentialAuthenticationService = userCredentialAuthenticationService;
    }

    public AuthSessionPayload login(LoginInput input) {
        AuthPrincipal principal = authenticate(input);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList());

        SecurityContext securityContext = securityContextHolderStrategy.createEmptyContext();
        securityContext.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(securityContext);

        ServletRequestAttributes requestAttributes = currentRequestAttributes();
        securityContextRepository.saveContext(
                securityContext,
                requestAttributes.getRequest(),
                requestAttributes.getResponse());

        return AuthSessionPayload.authenticated(principal);
    }

    @Transactional
    public AuthSessionPayload changeOwnPassword(ChangeOwnPasswordInput input) {
        AuthPrincipal currentPrincipal = currentPrincipal();
        AuthPrincipal refreshedPrincipal = userCredentialAuthenticationService.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DB-backed credentials are unavailable."))
                .changeOwnPassword(currentPrincipal, input);
        savePrincipal(refreshedPrincipal);
        return AuthSessionPayload.authenticated(refreshedPrincipal);
    }

    public AuthSessionPayload currentSession() {
        Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return AuthSessionPayload.anonymous();
        }

        return AuthSessionPayload.authenticated(principal);
    }

    public AuthSessionPayload logout() {
        ServletRequestAttributes requestAttributes = currentRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        HttpServletResponse response = requestAttributes.getResponse();

        SecurityContext emptyContext = securityContextHolderStrategy.createEmptyContext();
        securityContextHolderStrategy.setContext(emptyContext);
        securityContextRepository.saveContext(emptyContext, request, response);

        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        return AuthSessionPayload.anonymous();
    }

    private AuthPrincipal authenticate(LoginInput input) {
        try {
            Optional<AuthPrincipal> dbPrincipal = userCredentialAuthenticationService.stream()
                    .findFirst()
                    .flatMap(service -> service.authenticate(input));
            if (dbPrincipal.isPresent()) {
                return dbPrincipal.get();
            }
        } catch (BadCredentialsException ex) {
            Optional<AuthPrincipal> breakGlassPrincipal = breakGlassPrincipal(input);
            if (breakGlassPrincipal.isPresent()) {
                return breakGlassPrincipal.get();
            }
            throw ex;
        }

        return breakGlassPrincipal(input)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
    }

    private Optional<AuthPrincipal> breakGlassPrincipal(LoginInput input) {
        if (!properties.breakGlassEnabled()) {
            return Optional.empty();
        }

        return configuredLogin(input).map(login -> {
            LOGGER.warn("Baseline break-glass authentication accepted for {}.", normalize(login.email()));
            return configuredPrincipal(login);
        });
    }

    private Optional<ConfiguredLogin> configuredLogin(LoginInput input) {
        if (constantTimeEquals(normalize(input.email()), normalize(properties.username()))
                && constantTimeEquals(input.password(), properties.password())) {
            return Optional.of(new ConfiguredLogin(
                    properties.username(),
                    properties.displayName(),
                    properties.role()));
        }

        return properties.additionalUsers().stream()
                .filter(user -> constantTimeEquals(normalize(input.email()), normalize(user.email()))
                        && constantTimeEquals(input.password(), user.password()))
                .findFirst()
                .map(user -> new ConfiguredLogin(user.email(), user.displayName(), user.role()));
    }

    private AuthPrincipal configuredPrincipal(ConfiguredLogin login) {
        UserSessionIdentity identity = currentUserContextService.resolveSessionIdentity(
                login.email(),
                login.displayName(),
                login.role());
        return new AuthPrincipal(
                identity.id(),
                identity.email(),
                identity.displayName(),
                identity.roles());
    }

    private AuthPrincipal currentPrincipal() {
        Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthPrincipal principal)
                || !StringUtils.hasText(principal.id())) {
            throw new IllegalStateException("Authenticated principal is required.");
        }

        try {
            UUID.fromString(principal.id());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("DB-backed password change requires a persisted user principal.", ex);
        }
        return principal;
    }

    private void savePrincipal(AuthPrincipal principal) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList());

        SecurityContext securityContext = securityContextHolderStrategy.createEmptyContext();
        securityContext.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(securityContext);

        ServletRequestAttributes requestAttributes = currentRequestAttributes();
        securityContextRepository.saveContext(
                securityContext,
                requestAttributes.getRequest(),
                requestAttributes.getResponse());
    }

    private ServletRequestAttributes currentRequestAttributes() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes;
        }

        throw new IllegalStateException("Authentication requires an active servlet request.");
    }

    private String normalize(String value) {
        return StringUtils.trimWhitespace(value).toLowerCase(Locale.ROOT);
    }

    private boolean constantTimeEquals(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }

        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private record ConfiguredLogin(String email, String displayName, com.example.authstarter.security.AuthStarterRole role) {
    }
}
