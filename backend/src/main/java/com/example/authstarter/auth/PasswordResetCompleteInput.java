package com.example.authstarter.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompleteInput(
        @NotBlank String token,
        @NotBlank @Size(min = 12, max = 128) String newPassword) {
}
