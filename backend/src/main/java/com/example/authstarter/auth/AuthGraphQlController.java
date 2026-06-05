package com.example.authstarter.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class AuthGraphQlController {

    private final BaselineAuthService baselineAuthService;
    private final ObjectProvider<UserInvitationService> userInvitationService;
    private final ObjectProvider<PasswordResetService> passwordResetService;

    public AuthGraphQlController(
            BaselineAuthService baselineAuthService,
            ObjectProvider<UserInvitationService> userInvitationService,
            ObjectProvider<PasswordResetService> passwordResetService) {
        this.baselineAuthService = baselineAuthService;
        this.userInvitationService = userInvitationService;
        this.passwordResetService = passwordResetService;
    }

    @QueryMapping
    public AuthSessionPayload currentSession() {
        return baselineAuthService.currentSession();
    }

    @MutationMapping
    public AuthSessionPayload login(@Argument @Valid LoginInput input) {
        return baselineAuthService.login(input);
    }

    @MutationMapping
    public AuthSessionPayload logout() {
        return baselineAuthService.logout();
    }

    @MutationMapping
    public AuthSessionPayload changeOwnPassword(@Argument @Valid ChangeOwnPasswordInput input) {
        return baselineAuthService.changeOwnPassword(input);
    }

    @MutationMapping
    public InvitationPasswordSetupPayload acceptUserInvite(@Argument @Valid AcceptUserInviteInput input) {
        return userInvitationService.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User invitation is unavailable without auth persistence."))
                .acceptInvitation(input);
    }

    @MutationMapping
    public PasswordResetPayload requestPasswordReset(@Argument @Valid PasswordResetRequestInput input) {
        return passwordResetService.stream()
                .findFirst()
                .map(service -> service.requestReset(input))
                .orElse(new PasswordResetPayload(
                        "If a matching active account exists, a password reset email has been sent."));
    }

    @MutationMapping
    public PasswordResetPayload resetPassword(@Argument @Valid PasswordResetCompleteInput input) {
        return passwordResetService.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Password reset is unavailable without auth persistence."))
                .completeReset(input);
    }
}
