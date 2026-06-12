package com.example.authstarter.auth.mfa;

import java.util.List;

/** Plaintext recovery codes returned once, immediately after enrollment. */
public record MfaRecoveryCodesPayload(List<String> recoveryCodes) {
}
