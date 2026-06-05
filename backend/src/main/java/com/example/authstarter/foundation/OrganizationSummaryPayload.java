package com.example.authstarter.foundation;

public record OrganizationSummaryPayload(
        String id,
        String displayName,
        String legalName,
        String status,
        String workspaceId,
        String workspaceCode) {
}
