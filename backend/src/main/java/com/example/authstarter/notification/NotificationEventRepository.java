package com.example.authstarter.notification;

import com.example.authstarter.foundation.OrganizationContextPayload;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

interface NotificationEventRepository {

    void savePending(NotificationEventInsert event, OffsetDateTime createdAt);

    void markDelivered(UUID eventId, NotificationDeliveryResult result, OffsetDateTime updatedAt);

    List<NotificationEventPayload> listEvents(OrganizationContextPayload organization);
}

record NotificationEventInsert(
        UUID id,
        UUID organizationId,
        UUID workspaceId,
        String sourceType,
        UUID sourceId,
        String eventType,
        String recipientType,
        UUID recipientId,
        String recipientDisplayName,
        String recipientEmail,
        String provider) {
}

@Repository
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
class JdbcNotificationEventRepository implements NotificationEventRepository {

    private static final String CHANNEL = "EMAIL";
    private final JdbcClient jdbcClient;

    JdbcNotificationEventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void savePending(NotificationEventInsert event, OffsetDateTime createdAt) {
        jdbcClient.sql("""
                        insert into notification_events (
                            id,
                            organization_id,
                            workspace_id,
                            source_type,
                            source_id,
                            event_type,
                            recipient_type,
                            recipient_id,
                            recipient_display_name,
                            recipient_email,
                            channel,
                            provider,
                            delivery_status,
                            created_at,
                            updated_at
                        ) values (
                            :id,
                            :organizationId,
                            :workspaceId,
                            :sourceType,
                            :sourceId,
                            :eventType,
                            :recipientType,
                            :recipientId,
                            :recipientDisplayName,
                            :recipientEmail,
                            :channel,
                            :provider,
                            'PENDING',
                            :createdAt,
                            :createdAt
                        )
                        """)
                .param("id", event.id())
                .param("organizationId", event.organizationId())
                .param("workspaceId", event.workspaceId())
                .param("sourceType", event.sourceType())
                .param("sourceId", event.sourceId())
                .param("eventType", event.eventType())
                .param("recipientType", event.recipientType())
                .param("recipientId", event.recipientId())
                .param("recipientDisplayName", event.recipientDisplayName())
                .param("recipientEmail", event.recipientEmail())
                .param("channel", CHANNEL)
                .param("provider", event.provider())
                .param("createdAt", createdAt)
                .update();
    }

    @Override
    public void markDelivered(UUID eventId, NotificationDeliveryResult result, OffsetDateTime updatedAt) {
        jdbcClient.sql("""
                        update notification_events
                           set delivery_status = :deliveryStatus,
                               provider_message_id = :providerMessageId,
                               failure_reason = :failureReason,
                               sent_at = case when :deliveryStatus = 'SENT' then :updatedAt else null end,
                               updated_at = :updatedAt
                         where id = :eventId
                        """)
                .param("deliveryStatus", result.status())
                .param("providerMessageId", result.providerMessageId())
                .param("failureReason", result.failureReason())
                .param("updatedAt", updatedAt)
                .param("eventId", eventId)
                .update();
    }

    @Override
    public List<NotificationEventPayload> listEvents(OrganizationContextPayload organization) {
        return jdbcClient.sql("""
                        select id,
                               organization_id,
                               workspace_id,
                               source_type,
                               source_id,
                               event_type,
                               recipient_type,
                               recipient_id,
                               recipient_display_name,
                               recipient_email,
                               channel,
                               provider,
                               delivery_status,
                               provider_message_id,
                               failure_reason,
                               created_at,
                               sent_at,
                               updated_at
                          from notification_events
                         where organization_id = :organizationId
                           and workspace_id = :workspaceId
                         order by created_at desc, event_type, recipient_display_name
                        """)
                .param("organizationId", organizationId(organization))
                .param("workspaceId", workspaceId(organization))
                .query(this::mapEvent)
                .list();
    }

    private NotificationEventPayload mapEvent(ResultSet rs, int rowNumber) throws SQLException {
        return new NotificationEventPayload(
                rs.getString("id"),
                rs.getString("organization_id"),
                rs.getString("workspace_id"),
                rs.getString("source_type"),
                rs.getString("source_id"),
                rs.getString("event_type"),
                rs.getString("recipient_type"),
                rs.getString("recipient_id"),
                rs.getString("recipient_display_name"),
                rs.getString("recipient_email"),
                rs.getString("channel"),
                rs.getString("provider"),
                rs.getString("delivery_status"),
                rs.getString("provider_message_id"),
                rs.getString("failure_reason"),
                timestampToString(rs, "created_at"),
                timestampToString(rs, "sent_at"),
                timestampToString(rs, "updated_at"));
    }

    private UUID organizationId(OrganizationContextPayload organization) {
        return UUID.fromString(organization.organizationId());
    }

    private UUID workspaceId(OrganizationContextPayload organization) {
        return UUID.fromString(organization.workspaceId());
    }

    private String timestampToString(ResultSet rs, String columnName) throws SQLException {
        OffsetDateTime timestamp = rs.getObject(columnName, OffsetDateTime.class);
        return timestamp == null ? null : timestamp.toString();
    }
}
