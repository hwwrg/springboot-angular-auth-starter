package com.example.authstarter.foundation;

import java.io.Serializable;

public record OrganizationContextPayload(
        String organizationId,
        String organizationDisplayName,
        String organizationStatus,
        String workspaceId,
        String workspaceCode,
        String workspaceStatus,
        String role) implements Serializable {
}
