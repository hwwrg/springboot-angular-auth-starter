package com.example.authstarter.auth;

import jakarta.validation.constraints.NotBlank;

public record ChangeOwnPasswordInput(String currentPassword, @NotBlank String newPassword) {
}
