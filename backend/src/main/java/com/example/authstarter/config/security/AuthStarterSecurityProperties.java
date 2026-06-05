package com.example.authstarter.config.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authstarter.security")
public record AuthStarterSecurityProperties(List<String> allowedOrigins) {
}
