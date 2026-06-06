package com.example.authstarter.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginInput(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 128) String password) {
}
