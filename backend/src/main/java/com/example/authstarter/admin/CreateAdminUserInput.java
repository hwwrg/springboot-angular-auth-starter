package com.example.authstarter.admin;

public record CreateAdminUserInput(
        String email,
        String displayName,
        String userStatus,
        String role,
        String membershipStatus,
        Boolean primaryMembership) {
}
