package com.example.authstarter.security;

import java.util.List;

public record RbacBaselinePayload(
        String status,
        List<String> resolvedRoles) {
}
