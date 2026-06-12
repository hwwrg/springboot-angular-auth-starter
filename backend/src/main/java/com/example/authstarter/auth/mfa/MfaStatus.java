package com.example.authstarter.auth.mfa;

/**
 * Current MFA state for an account.
 *
 * @param enabled whether TOTP MFA is confirmed and enforced at login
 * @param pending whether an unconfirmed enrollment secret exists
 * @param remainingRecoveryCodes count of unused recovery codes
 */
public record MfaStatus(boolean enabled, boolean pending, int remainingRecoveryCodes) {

    public static MfaStatus disabled() {
        return new MfaStatus(false, false, 0);
    }
}
