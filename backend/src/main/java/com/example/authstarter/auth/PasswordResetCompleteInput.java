package com.example.authstarter.auth;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetCompleteInput(@NotBlank String token, @NotBlank String newPassword) {
}
