package com.example.authstarter.foundation;

import java.util.List;

public record CurrentUserProfilePayload(
        String id,
        String email,
        String displayName,
        String status,
        OrganizationContextPayload currentOrganization,
        List<OrganizationMembershipPayload> memberships) {
}
