package com.example.authstarter.admin;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.foundation.AuthenticatedPrincipalResolver;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AdminManagementGraphQlController {

    private final AuthenticatedPrincipalResolver principalResolver;
    private final AdminManagementService adminManagementService;

    public AdminManagementGraphQlController(
            AuthenticatedPrincipalResolver principalResolver,
            AdminManagementService adminManagementService) {
        this.principalResolver = principalResolver;
        this.adminManagementService = adminManagementService;
    }

    @QueryMapping
    public AdminManagementBaselinePayload adminManagementBaseline() {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return adminManagementService.adminManagementBaseline(principal);
    }

    @MutationMapping
    public AdminUserSummaryPayload adminCreateUser(@Argument CreateAdminUserInput input) {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return adminManagementService.createUser(principal, input);
    }

    @MutationMapping
    public AdminUserSummaryPayload adminUpdateUser(@Argument UpdateAdminUserInput input) {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return adminManagementService.updateUser(principal, input);
    }
}
