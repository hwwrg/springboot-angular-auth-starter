package com.example.authstarter.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@EnableConfigurationProperties(InvitationPasswordSetupUrlProperties.class)
public class InvitationPasswordSetupUrlBuilder {

    private final InvitationPasswordSetupUrlProperties properties;

    InvitationPasswordSetupUrlBuilder(InvitationPasswordSetupUrlProperties properties) {
        this.properties = properties;
    }

    public String setupUrl(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new IllegalArgumentException("Invitation token is required to build a password setup URL.");
        }
        String baseUrl = StringUtils.hasText(properties.getBaseUrl())
                ? properties.getBaseUrl().trim()
                : "http://localhost:4200/accept-invite";
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}

@ConfigurationProperties(prefix = "authstarter.auth.invitation.password-setup-url")
class InvitationPasswordSetupUrlProperties {

    private String baseUrl = "http://localhost:4200/accept-invite";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
