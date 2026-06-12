package com.example.authstarter.auth.mfa;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Generates human-friendly single-use recovery codes such as
 * {@code 7Q4F-9KD2-MHN8}. Ambiguous characters are excluded so codes are easy
 * to read and transcribe from a printout.
 */
@Service
public class RecoveryCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GROUPS = 3;
    private static final int GROUP_LENGTH = 4;

    private final SecureRandom secureRandom = new SecureRandom();

    public List<String> generate(int count) {
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            codes.add(generateOne());
        }
        return codes;
    }

    private String generateOne() {
        StringBuilder code = new StringBuilder(GROUPS * GROUP_LENGTH + GROUPS - 1);
        for (int group = 0; group < GROUPS; group++) {
            if (group > 0) {
                code.append('-');
            }
            for (int i = 0; i < GROUP_LENGTH; i++) {
                code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
            }
        }
        return code.toString();
    }

    /** Normalizes user-entered codes (case, spacing) before hashing/comparison. */
    public static String normalize(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toUpperCase(java.util.Locale.ROOT).replace(" ", "");
    }
}
