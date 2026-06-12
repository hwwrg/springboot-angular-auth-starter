package com.example.authstarter.auth.mfa;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * RFC 6238 time-based one-time password generation and verification using the
 * standard authenticator-app defaults (HMAC-SHA1, 30-second steps, 6 digits).
 */
@Service
public class TotpService {

    private static final int SECRET_BYTES = 20;
    private static final long TIME_STEP_SECONDS = 30L;
    private static final int DIGITS = 6;
    private static final int DIGITS_MODULO = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Generates a new random shared secret encoded as unpadded base32. */
    public String generateSecret() {
        byte[] buffer = new byte[SECRET_BYTES];
        secureRandom.nextBytes(buffer);
        return Base32.encode(buffer);
    }

    /**
     * Builds an {@code otpauth://} provisioning URI that authenticator apps can
     * import directly (typically rendered as a QR code by the frontend).
     */
    public String otpAuthUri(String issuer, String accountEmail, String base32Secret) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String label = URLEncoder.encode(issuer + ":" + accountEmail, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + TIME_STEP_SECONDS;
    }

    /**
     * Verifies a submitted code against the secret, accepting codes from
     * {@code stepTolerance} steps before and after the current step to absorb
     * clock drift.
     */
    public boolean verify(String base32Secret, String submittedCode, int stepTolerance) {
        return verifyAt(base32Secret, submittedCode, stepTolerance, System.currentTimeMillis() / 1000L);
    }

    boolean verifyAt(String base32Secret, String submittedCode, int stepTolerance, long epochSeconds) {
        if (!StringUtils.hasText(base32Secret) || !StringUtils.hasText(submittedCode)) {
            return false;
        }
        String normalized = submittedCode.trim().replace(" ", "");
        if (normalized.length() != DIGITS || !normalized.chars().allMatch(Character::isDigit)) {
            return false;
        }

        int expectedValue = Integer.parseInt(normalized);
        long currentStep = epochSeconds / TIME_STEP_SECONDS;
        byte[] key = Base32.decode(base32Secret);
        for (long step = currentStep - stepTolerance; step <= currentStep + stepTolerance; step++) {
            if (constantTimeEquals(generateCode(key, step), expectedValue)) {
                return true;
            }
        }
        return false;
    }

    private int generateCode(byte[] key, long step) {
        byte[] data = new byte[8];
        long value = step;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return binary % DIGITS_MODULO;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA1 is unavailable for TOTP generation.", ex);
        }
    }

    private boolean constantTimeEquals(int generated, int expected) {
        return (generated ^ expected) == 0;
    }
}
