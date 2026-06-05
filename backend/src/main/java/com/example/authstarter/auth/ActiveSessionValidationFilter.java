package com.example.authstarter.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ActiveSessionValidationFilter extends OncePerRequestFilter {

    private final AuthenticatedSessionValidationService sessionValidationService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public ActiveSessionValidationFilter(AuthenticatedSessionValidationService sessionValidationService) {
        this.sessionValidationService = sessionValidationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthPrincipal)) {
            filterChain.doFilter(request, response);
            return;
        }

        sessionValidationService.refresh(authentication).ifPresentOrElse(refreshedAuthentication -> {
            SecurityContext securityContext = securityContextHolderStrategy.createEmptyContext();
            securityContext.setAuthentication(refreshedAuthentication);
            securityContextHolderStrategy.setContext(securityContext);
            securityContextRepository.saveContext(securityContext, request, response);
        }, () -> invalidateSession(request, response));

        filterChain.doFilter(request, response);
    }

    private void invalidateSession(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext emptyContext = securityContextHolderStrategy.createEmptyContext();
        securityContextHolderStrategy.setContext(emptyContext);
        securityContextRepository.saveContext(emptyContext, request, response);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }
}
