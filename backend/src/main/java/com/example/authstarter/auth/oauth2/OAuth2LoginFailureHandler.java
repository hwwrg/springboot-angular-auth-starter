package com.example.authstarter.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Sends failed or cancelled provider logins back to the frontend login page
 * with a generic error marker instead of exposing provider error details.
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final OAuth2LoginProperties properties;

    public OAuth2LoginFailureHandler(OAuth2LoginProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        LOGGER.info("OAuth2 login failed: {}", exception.getMessage());
        response.sendRedirect(OAuth2LoginSuccessHandler.redirectWithError(properties.failureRedirectUrl(), "oauth2"));
    }
}
