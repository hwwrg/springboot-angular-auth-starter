package com.example.authstarter.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Security")
public class CsrfController {

    private static final String DEFAULT_CSRF_ATTRIBUTE_NAME = "_csrf";

    @Operation(
            summary = "Fetch a CSRF token",
            description = "Returns the CSRF header name, parameter name, and token value required by browser clients before calling unsafe endpoints such as POST /graphql.")
    @GetMapping("/auth/csrf")
    CsrfPayload csrf(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute(DEFAULT_CSRF_ATTRIBUTE_NAME);
        }
        if (csrfToken == null) {
            throw new IllegalStateException("CSRF token is not available on the current request.");
        }
        return new CsrfPayload(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken());
    }
}
