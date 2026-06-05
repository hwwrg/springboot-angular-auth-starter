package com.example.authstarter.auth;

import java.io.Serializable;
import java.util.List;

public record AuthPrincipal(
        String id,
        String email,
        String displayName,
        List<String> roles,
        boolean mustChangePassword) implements Serializable {

    public AuthPrincipal(String id, String email, String displayName, List<String> roles) {
        this(id, email, displayName, roles, false);
    }
}
