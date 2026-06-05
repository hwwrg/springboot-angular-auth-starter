package com.example.authstarter.auth;

public record InvitationPasswordSetupPayload(
        String userId,
        String email,
        String status) {
}
