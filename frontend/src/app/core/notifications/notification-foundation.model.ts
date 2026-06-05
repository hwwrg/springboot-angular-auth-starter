export interface NotificationEvent {
  id: string;
  organizationId: string;
  workspaceId: string;
  sourceType: string;
  sourceId: string;
  eventType: string;
  recipientType: string;
  recipientId: string | null;
  recipientDisplayName: string;
  recipientEmail: string | null;
  channel: string;
  provider: string;
  deliveryStatus: string;
  providerMessageId: string | null;
  failureReason: string | null;
  createdAt: string;
  sentAt: string | null;
  updatedAt: string;
}
