package com.example.authstarter.auth.mfa;

/**
 * Data returned when an enrollment is started: the shared secret (for manual
 * entry) and an otpauth URI the frontend can render as a QR code.
 */
public record MfaEnrollment(String secret, String otpAuthUri) {
}
