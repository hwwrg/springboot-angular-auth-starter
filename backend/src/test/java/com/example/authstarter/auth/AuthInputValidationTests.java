package com.example.authstarter.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class AuthInputValidationTests {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void tearDown() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptInvitePasswordKeepsTwelveToOneHundredTwentyEightCharacterRange() {
        assertThat(VALIDATOR.validate(new AcceptUserInviteInput("token", "short")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword");
        assertThat(VALIDATOR.validate(new AcceptUserInviteInput("token", "a".repeat(12)))).isEmpty();
        assertThat(VALIDATOR.validate(new AcceptUserInviteInput("token", "a".repeat(129))))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword");
    }

    @Test
    void resetPasswordRequiresTwelveToOneHundredTwentyEightCharacterPassword() {
        assertThat(VALIDATOR.validate(new PasswordResetCompleteInput("token", "short")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword");
        assertThat(VALIDATOR.validate(new PasswordResetCompleteInput("token", "a".repeat(12)))).isEmpty();
        assertThat(VALIDATOR.validate(new PasswordResetCompleteInput("token", "a".repeat(129))))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword");
    }

    @Test
    void changeOwnPasswordRequiresTwelveToOneHundredTwentyEightCharacterPassword() {
        assertThat(VALIDATOR.validate(new ChangeOwnPasswordInput("current-password", "short")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword");
        assertThat(VALIDATOR.validate(new ChangeOwnPasswordInput("current-password", "a".repeat(12)))).isEmpty();
        assertThat(VALIDATOR.validate(new ChangeOwnPasswordInput("current-password", "a".repeat(129))))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword");
    }

    @Test
    void loginPasswordAllowsOnlyUpToOneHundredTwentyEightCharacters() {
        assertThat(VALIDATOR.validate(new LoginInput("operator@example.test", "a".repeat(128)))).isEmpty();

        Set<String> invalidFields = VALIDATOR.validate(new LoginInput("operator@example.test", "a".repeat(129)))
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).contains("password");
    }
}
