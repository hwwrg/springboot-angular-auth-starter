package com.example.authstarter.foundation;

import java.util.List;

public record UserSessionIdentity(
        String id,
        String email,
        String displayName,
        List<String> roles) {
}
