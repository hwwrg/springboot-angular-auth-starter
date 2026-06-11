package com.example.authstarter.auth.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redirect targets used after an OAuth2/OIDC login attempt. Both default to the
 * local frontend so the starter works out of the box with the Angular dev
 * server.
 */
@ConfigurationProperties(prefix = "authstarter.auth.oauth2")
public record OAuth2LoginProperties(String successRedirectUrl, String failureRedirectUrl) {

    public OAuth2LoginProperties {
        if (successRedirectUrl == null || successRedirectUrl.isBlank()) {
            successRedirectUrl = "http://localhost:4200/app/dashboard";
        }
        if (failureRedirectUrl == null || failureRedirectUrl.isBlank()) {
            failureRedirectUrl = "http://localhost:4200/login";
        }
    }
}
