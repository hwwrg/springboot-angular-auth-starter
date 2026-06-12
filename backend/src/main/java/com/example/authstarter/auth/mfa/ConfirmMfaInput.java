package com.example.authstarter.auth.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmMfaInput(@NotBlank @Size(max = 32) String code) {
}
