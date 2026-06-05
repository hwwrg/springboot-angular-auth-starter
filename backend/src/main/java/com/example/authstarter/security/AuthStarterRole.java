package com.example.authstarter.security;

import java.util.Arrays;

public enum AuthStarterRole {
    SUPERADMIN,
    ORG_ADMIN,
    USER;

    public String authority() {
        return "ROLE_" + name();
    }

    public static AuthStarterRole fromSessionValue(String value) {
        return Arrays.stream(values())
                .filter(role -> role.name().equals(value) || role.authority().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported auth starter role: " + value));
    }
}
