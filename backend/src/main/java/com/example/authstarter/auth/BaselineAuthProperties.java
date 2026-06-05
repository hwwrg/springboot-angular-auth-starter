package com.example.authstarter.auth;

import com.example.authstarter.security.AuthStarterRole;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authstarter.auth.baseline")
public record BaselineAuthProperties(
        String username,
        String password,
        String displayName,
        AuthStarterRole role,
        boolean breakGlassEnabled,
        List<AdditionalUser> additionalUsers) {

    public BaselineAuthProperties {
        additionalUsers = additionalUsers == null ? List.of() : List.copyOf(additionalUsers);
    }

    public record AdditionalUser(
            String email,
            String password,
            String displayName,
            AuthStarterRole role) {
    }
}
