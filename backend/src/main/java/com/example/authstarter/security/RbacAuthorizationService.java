package com.example.authstarter.security;

import com.example.authstarter.auth.AuthenticatedRoleResolver;
import java.util.Arrays;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class RbacAuthorizationService {

    private final AuthenticatedRoleResolver roleResolver;

    public RbacAuthorizationService(AuthenticatedRoleResolver roleResolver) {
        this.roleResolver = roleResolver;
    }

    public Set<AuthStarterRole> requireAnyRole(AuthStarterRole... allowedRoles) {
        Set<AuthStarterRole> currentRoles = roleResolver.resolveCurrentRoles();
        boolean authorized = Arrays.stream(allowedRoles).anyMatch(currentRoles::contains);
        if (!authorized) {
            throw new AccessDeniedException("The authenticated session does not have the required role.");
        }

        return currentRoles;
    }
}
