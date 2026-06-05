package com.example.authstarter.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestInput(@NotBlank @Email String email) {
}
