package com.example.authstarter.notification;

public record NotificationEventPayload(
        String id,
        String organizationId,
        String workspaceId,
        String sourceType,
        String sourceId,
        String eventType,
        String recipientType,
        String recipientId,
        String recipientDisplayName,
        String recipientEmail,
        String channel,
        String provider,
        String deliveryStatus,
        String providerMessageId,
        String failureReason,
        String createdAt,
        String sentAt,
        String updatedAt) {
}
