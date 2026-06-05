package com.example.authstarter.admin;

public record AdminUserSummaryPayload(
        String id,
        String email,
        String displayName,
        String status,
        String role,
        String membershipStatus,
        boolean primaryMembership,
        String createdAt,
        String updatedAt) {
}
