package com.example.authstarter.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserInvitationTokenPayload(
        UUID tokenId,
        UUID userId,
        String rawToken,
        OffsetDateTime expiresAt,
        long expiresInDays) {
}
