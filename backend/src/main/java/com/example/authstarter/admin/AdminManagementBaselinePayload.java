package com.example.authstarter.admin;

import com.example.authstarter.foundation.OrganizationContextPayload;
import com.example.authstarter.notification.NotificationEventPayload;
import java.util.List;

public record AdminManagementBaselinePayload(
        OrganizationContextPayload currentOrganization,
        List<AdminUserSummaryPayload> users,
        List<NotificationEventPayload> notificationEvents,
        AdminManagementTotalsPayload totals) {
}
