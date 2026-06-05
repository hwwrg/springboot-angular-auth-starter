package com.example.authstarter.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@EnableConfigurationProperties(PasswordResetUrlProperties.class)
public class PasswordResetUrlBuilder {

    private final PasswordResetUrlProperties properties;

    PasswordResetUrlBuilder(PasswordResetUrlProperties properties) {
        this.properties = properties;
    }

    public String resetUrl(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new IllegalArgumentException("Password reset token is required to build a reset URL.");
        }
        String baseUrl = StringUtils.hasText(properties.getBaseUrl())
                ? properties.getBaseUrl().trim()
                : "http://localhost:4200/reset-password";
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}

@ConfigurationProperties(prefix = "authstarter.auth.password-reset.url")
class PasswordResetUrlProperties {

    private String baseUrl = "http://localhost:4200/reset-password";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
