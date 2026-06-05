package com.example.authstarter.admin;

public record UpdateAdminUserInput(
        String id,
        String displayName,
        String userStatus,
        String role,
        String membershipStatus,
        Boolean primaryMembership) {
}
