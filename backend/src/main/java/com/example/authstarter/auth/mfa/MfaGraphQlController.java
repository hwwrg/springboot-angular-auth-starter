package com.example.authstarter.auth.mfa;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.foundation.AuthenticatedPrincipalResolver;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

/**
 * Authenticated self-service MFA operations: status, enrollment, confirmation,
 * and disabling. All operate on the current session principal.
 */
@Controller
@Validated
public class MfaGraphQlController {

    private final AuthenticatedPrincipalResolver principalResolver;
    private final ObjectProvider<UserMfaService> userMfaService;

    public MfaGraphQlController(
            AuthenticatedPrincipalResolver principalResolver, ObjectProvider<UserMfaService> userMfaService) {
        this.principalResolver = principalResolver;
        this.userMfaService = userMfaService;
    }

    @QueryMapping
    public MfaStatus mfaStatus() {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return service().status(UUID.fromString(principal.id()));
    }

    @MutationMapping
    public MfaEnrollment startMfaEnrollment() {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return service().startEnrollment(UUID.fromString(principal.id()), principal.email());
    }

    @MutationMapping
    public MfaRecoveryCodesPayload confirmMfaEnrollment(@Argument @Valid ConfirmMfaInput input) {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        List<String> codes = service().confirmEnrollment(UUID.fromString(principal.id()), input.code());
        return new MfaRecoveryCodesPayload(codes);
    }

    @MutationMapping
    public MfaStatus disableMfa() {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return service().disable(UUID.fromString(principal.id()));
    }

    private UserMfaService service() {
        UserMfaService service = userMfaService.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Multi-factor authentication is unavailable without auth persistence.");
        }
        return service;
    }
}
