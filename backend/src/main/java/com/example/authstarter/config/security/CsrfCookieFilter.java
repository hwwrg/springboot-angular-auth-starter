package com.example.authstarter.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

final class CsrfCookieFilter extends OncePerRequestFilter {

    private static final String DEFAULT_CSRF_ATTRIBUTE_NAME = "_csrf";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute(DEFAULT_CSRF_ATTRIBUTE_NAME);
        }
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }
}
