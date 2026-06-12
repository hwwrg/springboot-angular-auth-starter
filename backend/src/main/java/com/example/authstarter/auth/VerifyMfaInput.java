package com.example.authstarter.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyMfaInput(@NotBlank @Size(max = 32) String code) {
}
