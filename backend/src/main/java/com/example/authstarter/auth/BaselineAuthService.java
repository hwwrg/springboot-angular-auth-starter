package com.example.authstarter.auth;

import com.example.authstarter.auth.mfa.UserMfaService;
import com.example.authstarter.foundation.CurrentUserContextService;
import com.example.authstarter.foundation.UserSessionIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

    private static final String PENDING_MFA_PRINCIPAL_ATTRIBUTE =
            BaselineAuthService.class.getName() + ".PENDING_MFA_PRINCIPAL";
    private static final String PENDING_MFA_CREATED_AT_ATTRIBUTE =
            BaselineAuthService.class.getName() + ".PENDING_MFA_CREATED_AT";
    private static final long PENDING_MFA_TTL_MILLIS = 5 * 60 * 1000L;

    private final BaselineAuthProperties properties;
    private final CurrentUserContextService currentUserContextService;
    private final ObjectProvider<UserCredentialAuthenticationService> userCredentialAuthenticationService;
    private final ObjectProvider<UserMfaService> userMfaService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public BaselineAuthService(
            BaselineAuthProperties properties,
            CurrentUserContextService currentUserContextService,
            ObjectProvider<UserCredentialAuthenticationService> userCredentialAuthenticationService,
            ObjectProvider<UserMfaService> userMfaService) {
        this.properties = properties;
        this.currentUserContextService = currentUserContextService;
        this.userCredentialAuthenticationService = userCredentialAuthenticationService;
        this.userMfaService = userMfaService;
    }

    public AuthSessionPayload login(LoginInput input) {
        AuthPrincipal principal = authenticate(input);

        if (requiresMfa(principal)) {
            storePendingMfaPrincipal(principal);
            return AuthSessionPayload.mfaChallenge();
        }

        establishSession(principal);
        return AuthSessionPayload.authenticated(principal);
    }

    /**
     * Completes a login that was paused for a second factor: validates the
     * submitted TOTP or recovery code against the pending principal stored in
     * the session and, on success, establishes the authenticated session.
     */
    public AuthSessionPayload verifyMfa(VerifyMfaInput input) {
        AuthPrincipal pendingPrincipal = consumePendingMfaPrincipal();
        UserMfaService mfaService = userMfaService.getIfAvailable();
        if (mfaService == null
                || !mfaService.verifyChallenge(UUID.fromString(pendingPrincipal.id()), input.code())) {
            storePendingMfaPrincipal(pendingPrincipal);
            throw new BadCredentialsException("The verification code is incorrect.");
        }

        establishSession(pendingPrincipal);
        return AuthSessionPayload.authenticated(pendingPrincipal);
    }

    private void establishSession(AuthPrincipal principal) {
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

    private boolean requiresMfa(AuthPrincipal principal) {
        UserMfaService mfaService = userMfaService.getIfAvailable();
        if (mfaService == null || !isPersistedUser(principal)) {
            return false;
        }
        return mfaService.isMfaEnabled(UUID.fromString(principal.id()));
    }

    private boolean isPersistedUser(AuthPrincipal principal) {
        if (!StringUtils.hasText(principal.id())) {
            return false;
        }
        try {
            UUID.fromString(principal.id());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void storePendingMfaPrincipal(AuthPrincipal principal) {
        HttpSession session = currentRequestAttributes().getRequest().getSession(true);
        session.setAttribute(PENDING_MFA_PRINCIPAL_ATTRIBUTE, principal);
        session.setAttribute(PENDING_MFA_CREATED_AT_ATTRIBUTE, System.currentTimeMillis());
    }

    private AuthPrincipal consumePendingMfaPrincipal() {
        HttpSession session = currentRequestAttributes().getRequest().getSession(false);
        if (session == null) {
            throw new BadCredentialsException("No multi-factor challenge is in progress.");
        }
        Object principal = session.getAttribute(PENDING_MFA_PRINCIPAL_ATTRIBUTE);
        Object createdAt = session.getAttribute(PENDING_MFA_CREATED_AT_ATTRIBUTE);
        session.removeAttribute(PENDING_MFA_PRINCIPAL_ATTRIBUTE);
        session.removeAttribute(PENDING_MFA_CREATED_AT_ATTRIBUTE);

        if (!(principal instanceof AuthPrincipal pendingPrincipal)
                || !(createdAt instanceof Long createdAtMillis)
                || System.currentTimeMillis() - createdAtMillis > PENDING_MFA_TTL_MILLIS) {
            throw new BadCredentialsException("The multi-factor challenge has expired. Sign in again.");
        }
        return pendingPrincipal;
    }

    @Transactional
    public AuthSessionPayload changeOwnPassword(ChangeOwnPasswordInput input) {
        AuthPrincipal currentPrincipal = currentPrincipal();
        AuthPrincipal refreshedPrincipal = userCredentialAuthenticationService.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DB-backed credentials are unavailable."))
                .changeOwnPassword(currentPrincipal, input);
        establishSession(refreshedPrincipal);
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
