package com.example.authstarter.config.graphql;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authstarter.graphql")
public record AuthStarterGraphQlProperties(
        int maxQueryDepth,
        int maxQueryComplexity,
        int maxRequestBytes,
        boolean introspectionEnabled) {

    public AuthStarterGraphQlProperties {
        maxQueryDepth = maxQueryDepth > 0 ? maxQueryDepth : 12;
        maxQueryComplexity = maxQueryComplexity > 0 ? maxQueryComplexity : 200;
        maxRequestBytes = maxRequestBytes > 0 ? maxRequestBytes : 32_768;
    }
}
