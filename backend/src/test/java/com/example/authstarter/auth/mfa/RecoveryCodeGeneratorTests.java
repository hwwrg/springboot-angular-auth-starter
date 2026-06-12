package com.example.authstarter.auth.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecoveryCodeGeneratorTests {

    private final RecoveryCodeGenerator generator = new RecoveryCodeGenerator();

    @Test
    void generatesTheRequestedNumberOfGroupedCodes() {
        List<String> codes = generator.generate(10);

        assertThat(codes).hasSize(10);
        assertThat(codes).allMatch(code -> code.matches("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}"));
        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    void excludesAmbiguousCharacters() {
        List<String> codes = generator.generate(50);

        assertThat(codes).allSatisfy(code -> assertThat(code.replace("-", "")).doesNotContainAnyWhitespaces());
        assertThat(String.join("", codes).replace("-", "")).doesNotContain("O", "0", "I", "1");
    }

    @Test
    void normalizesUserEnteredCodes() {
        assertThat(RecoveryCodeGenerator.normalize("  7q4f-9kd2-mhn8 ")).isEqualTo("7Q4F-9KD2-MHN8");
        assertThat(RecoveryCodeGenerator.normalize(null)).isEmpty();
    }
}
