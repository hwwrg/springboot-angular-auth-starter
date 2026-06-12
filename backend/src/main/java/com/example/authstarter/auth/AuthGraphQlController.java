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
    private final PublicAuthRateLimiter rateLimiter;

    public AuthGraphQlController(
            BaselineAuthService baselineAuthService,
            ObjectProvider<UserInvitationService> userInvitationService,
            ObjectProvider<PasswordResetService> passwordResetService,
            PublicAuthRateLimiter rateLimiter) {
        this.baselineAuthService = baselineAuthService;
        this.userInvitationService = userInvitationService;
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    @QueryMapping
    public AuthSessionPayload currentSession() {
        return baselineAuthService.currentSession();
    }

    @MutationMapping
    public AuthSessionPayload login(@Argument @Valid LoginInput input) {
        rateLimiter.checkEmail(PublicAuthRateLimiter.PublicAuthFlow.LOGIN, input == null ? null : input.email());
        return baselineAuthService.login(input);
    }

    @MutationMapping
    public AuthSessionPayload verifyMfa(@Argument @Valid VerifyMfaInput input) {
        rateLimiter.checkClient(PublicAuthRateLimiter.PublicAuthFlow.VERIFY_MFA);
        return baselineAuthService.verifyMfa(input);
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
        rateLimiter.checkToken(PublicAuthRateLimiter.PublicAuthFlow.ACCEPT_USER_INVITE, input == null ? null : input.token());
        return userInvitationService.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User invitation is unavailable without auth persistence."))
                .acceptInvitation(input);
    }

    @MutationMapping
    public PasswordResetPayload requestPasswordReset(@Argument @Valid PasswordResetRequestInput input) {
        rateLimiter.checkEmail(
                PublicAuthRateLimiter.PublicAuthFlow.REQUEST_PASSWORD_RESET,
                input == null ? null : input.email());
        return passwordResetService.stream()
                .findFirst()
                .map(service -> service.requestReset(input))
                .orElse(new PasswordResetPayload(
                        "If a matching active account exists, a password reset email has been sent."));
    }

    @MutationMapping
    public PasswordResetPayload resetPassword(@Argument @Valid PasswordResetCompleteInput input) {
        rateLimiter.checkToken(PublicAuthRateLimiter.PublicAuthFlow.RESET_PASSWORD, input == null ? null : input.token());
        return passwordResetService.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Password reset is unavailable without auth persistence."))
                .completeReset(input);
    }
}
