package com.example.authstarter.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeOwnPasswordInput(String currentPassword, @NotBlank @Size(min = 12, max = 128) String newPassword) {
}
