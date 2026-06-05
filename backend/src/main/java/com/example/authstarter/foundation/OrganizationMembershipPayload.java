package com.example.authstarter.foundation;

import java.io.Serializable;

public record OrganizationMembershipPayload(
        String organizationId,
        String organizationDisplayName,
        String organizationStatus,
        String workspaceId,
        String workspaceCode,
        String role,
        String status,
        boolean primaryMembership) implements Serializable {
}
