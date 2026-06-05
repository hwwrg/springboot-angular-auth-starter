package com.example.authstarter.security;

import java.util.Comparator;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class RbacBaselineGraphQlController {

    private final RbacAuthorizationService authorizationService;

    public RbacBaselineGraphQlController(RbacAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @QueryMapping
    public RbacBaselinePayload rbacBaseline() {
        return new RbacBaselinePayload(
                "RBAC_BASELINE_READY",
                authorizationService.requireAnyRole(AuthStarterRole.SUPERADMIN, AuthStarterRole.ORG_ADMIN)
                        .stream()
                        .sorted(Comparator.comparing(Enum::name))
                        .map(AuthStarterRole::name)
                        .toList());
    }
}
