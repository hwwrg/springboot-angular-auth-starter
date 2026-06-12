package com.example.authstarter.auth.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TotpServiceTests {

    private final TotpService totpService = new TotpService();

    @Test
    void generatesImportableBase32SecretsAndOtpAuthUri() {
        String secret = totpService.generateSecret();

        assertThat(secret).isNotBlank();
        assertThat(secret).matches("[A-Z2-7]+");

        String uri = totpService.otpAuthUri("Acme Auth", "user@example.test", secret);
        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("secret=" + secret);
        assertThat(uri).contains("issuer=Acme+Auth");
        assertThat(uri).contains("digits=6");
        assertThat(uri).contains("period=30");
    }

    @Test
    void acceptsTheCodeForTheCurrentTimeStep() {
        // RFC 6238 test vector secret "12345678901234567890" base32-encoded.
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        // Known TOTP value for this secret at T=59s (time step 1) is 287082.
        assertThat(totpService.verifyAt(secret, "287082", 0, 59L)).isTrue();
    }

    @Test
    void rejectsCodesOutsideTheToleranceWindow() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        assertThat(totpService.verifyAt(secret, "287082", 0, 59L + 120L)).isFalse();
    }

    @Test
    void rejectsMalformedCodes() {
        String secret = totpService.generateSecret();
        assertThat(totpService.verify(secret, "12ab56", 1)).isFalse();
        assertThat(totpService.verify(secret, "12345", 1)).isFalse();
        assertThat(totpService.verify(secret, "", 1)).isFalse();
    }
}
